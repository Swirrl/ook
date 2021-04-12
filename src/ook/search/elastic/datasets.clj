(ns ook.search.elastic.datasets
  (:require
   [clojure.set :as set]
   [meta-merge.core :as mm]
   [clojurewerkz.elastisch.query :as q]
   [clojurewerkz.elastisch.rest.document :as esd]
   [ook.search.elastic.util :as esu]
   [ook.util :as u]
   [ook.search.elastic.facets :as facets]
   [ook.search.elastic.components :as components]
   [ook.util :as util]
   [ook.search.elastic.codes :as codes]))

(defn- flatten-description-lang-strings [m]
  (-> m
      (assoc :description (-> m :dcterms:description :value))
      (dissoc :dcterms:description)))

(defn- clean-datasets-result [result]
  (->> result :hits :hits
       (map :_source)
       (map esu/normalize-keys)
       (map flatten-description-lang-strings)))

(defn all [{:keys [elastic/endpoint]}]
  (-> (esu/get-connection endpoint)
      (esd/search "dataset" "_doc" {:query (q/match-all)
                                    :size 500})
      clean-datasets-result))

(defn for-components [components {:keys [elastic/endpoint] :as opts}]
  (let [conn (esu/get-connection endpoint)]
    (->> (esd/search conn "dataset" "_doc"
                    {:query {:terms {:component components}}})
         :hits :hits (map :_source))))

(defn observation-query
  "Creates a query for finding observations with selected dimensions and dimension-values.

  Provides counts by dataset and collapses to a few example observations for each dataset."
  [dimension-selections]
  (let [dimension-selections (reduce ;; append .@id to reach into nested dimval docs
                              (fn [m [k v]] (assoc m (str k ".@id") v))
                              {}
                              dimension-selections)
        dimensions (keys dimension-selections)
        criteria (map (fn [[dim codes]]
                        (if (empty? codes)
                          {:exists {:field dim}} ;; dimension (with no codes specified) ought to be present
                          {:terms {dim codes}})) dimension-selections) ;; dimensions' values are one of the specified codes
        ]
    {:collapse {:field "qb:dataSet.@id"}
     :fields dimensions
     :_source false
     :query {:bool {:should criteria}}
     :aggregations {:datasets {:terms {:field "qb:dataSet.@id"}}}}))

(defn find-observations
  "Finds observations with given dimensions and dimension-values. Groups results by dataset."
  [dimension-selections {:keys [elastic/endpoint] :as opts}]
  (let [conn (esu/get-connection endpoint)]
    (esd/search conn "observation" "_doc"
                (observation-query dimension-selections))))

(defn datasets-from-observation-hits
  "Parses observation-query results to return a sequence of datasets."
  [observation-hits]
  (let [examples (->> observation-hits :hits :hits
                      (reduce (fn [matches {:keys [fields]}]
                                ;; Fields always returned as arrays even though obs properties (dimvals)
                                ;; are scalar. Here we extract the first value from each to unbox them.
                                (let [fields (zipmap (keys fields) (map first (vals fields)))]
                                  (assoc matches
                                         (get fields (keyword "qb:dataSet.@id"))
                                         {:matching_observation_example (dissoc fields (keyword "qb:dataSet.@id"))})))
                              {}))
        buckets (->> observation-hits :aggregations :datasets :buckets
                     (reduce (fn [matches {:keys [key doc_count]}]
                               (assoc matches
                                      key
                                      {:matching_observation_count doc_count}))
                             {}))]
    (map (fn [[id description]] (merge {:ook/uri id} description))
         (merge-with merge buckets examples))))

(defn code-uris-from-observation-hits
  "Parses observation-query results to return a sequence of code-uris."
  [observation-hits]
  (->> observation-hits :hits :hits
       (mapcat (fn [hit]
                 (-> hit
                     :fields
                     (dissoc (keyword "qb:dataSet.@id"))
                     vals
                     ((partial map first)))))
       distinct))

(defn explain-match
  "Replace matching observation example with per facet, per dimension, codelist and code summary"
  [datasets facets codelists codes]
  (let [code-lookup (util/id-lookup codes)
        codelist-lookup (util/id-lookup codelists)]
    (for [{:keys [matching_observation_example] :as dataset} datasets]
      (-> dataset
          (dissoc :matching_observation_example)
          (assoc :facets (for [{:keys [name dimensions]} facets]
                           {:name name
                            :dimensions (for [d dimensions]
                                          (let [code-uris [(get matching_observation_example
                                                                (keyword (str d ".@id")))]
                                                matches
                                                (->>
                                                 code-uris
                                                 (map code-lookup)
                                                 (partition-by :scheme)
                                                 (map (fn [codes]
                                                        (assoc (codelist-lookup (-> codes first :scheme))
                                                               :examples
                                                               (map #(dissoc % :scheme) codes)))))]
                                            {:ook/uri d
                                             :codelists matches
                                             }))}))))))

(defn for-facets [selections {:keys [elastic/endpoint] :as opts}]
  (let [codelist-uris (mapcat keys (vals selections))
        dimensions-lookup (components/codelist-to-dimensions-lookup codelist-uris opts)
        dimension-selections (facets/dimension-selections selections dimensions-lookup)
        observation-hits (find-observations dimension-selections opts)
        dataset-hits (datasets-from-observation-hits observation-hits)
        code-uris (code-uris-from-observation-hits observation-hits)
        facets (facets/get-facets-for-selections selections opts)
        codelists (components/get-codelists codelist-uris opts)
        codes (codes/get-codes code-uris opts)]
    (->>
     (explain-match dataset-hits facets codelists codes)
     (map esu/normalize-keys)
     (map flatten-description-lang-strings))))

(defn total-count [{:keys [elastic/endpoint]}]
  (-> (esu/get-connection endpoint)
      (esd/count "dataset" "_doc")
      :count))

(comment
  (def conn (esu/get-connection "http://localhost:9200")))
