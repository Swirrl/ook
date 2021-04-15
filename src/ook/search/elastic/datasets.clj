(ns ook.search.elastic.datasets
  (:require
   [clojurewerkz.elastisch.query :as q]
   [clojurewerkz.elastisch.rest.document :as esd]
   [ook.search.elastic.util :as esu]
   [ook.util :as util]
   [ook.search.elastic.facets :as facets]
   [ook.search.elastic.components :as components]
   [ook.search.elastic.codes :as codes]
   [ook.util :as u]))

(defn- clean-datasets-result [result]
  (->> result :hits :hits
       (map :_source)
       (map esu/normalize-keys)))

(defn all [{:keys [elastic/endpoint]}]
  (-> (esu/get-connection endpoint)
      (esd/search "dataset" "_doc" {:query (q/match-all)
                                    :size 500})
      clean-datasets-result))

(defn get-datasets
  "Find datasets using their URIs."
  [uris {:keys [elastic/endpoint]}]
  (let [conn (esu/get-connection endpoint)
        uris (util/box uris)]
    (->> (esd/search conn "dataset" "_doc"
                     {:query (q/ids "_doc" uris)
                      :size (count uris)})
         clean-datasets-result)))

(defn for-components [components {:keys [elastic/endpoint] :as opts}]
  (let [conn (esu/get-connection endpoint)]
    (->> (esd/search conn "dataset" "_doc"
                     {:query {:terms {:component components}}})
         :hits :hits (map :_source))))

(defn for-cubes
  "Find datasets for cube URIs"
  [cube-uris {:keys [elastic/endpoint]}]
  (let [conn (esu/get-connection endpoint)
        uris (util/box cube-uris)]
    (->> (esd/search conn "dataset" "_doc"
                     {:query {:terms {:cube uris}}
                      :size (count uris)})
         clean-datasets-result)))

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
                                         {:matching-observation-example (dissoc fields (keyword "qb:dataSet.@id"))})))
                              {}))
        buckets (->> observation-hits :aggregations :datasets :buckets
                     (reduce (fn [matches {:keys [key doc_count]}]
                               (assoc matches
                                      key
                                      {:matching-observation-count doc_count}))
                             {}))]
    (map (fn [[id match-description]] (merge {:ook/uri id} match-description))
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

(defn explain-dimensions [matching-observation-example dimensions codelist-lookup code-lookup]
  (->> dimensions
       (map (fn [dimension]
              (let [code-uris (some-> matching-observation-example
                                      (get (keyword (str (:ook/uri dimension) ".@id")))
                                      util/box)
                    matches (some->> code-uris
                                     (map code-lookup)
                                     (map (fn [code] (update code :scheme #(->> % util/box (map codelist-lookup))))))]
                (when matches
                  (-> dimension
                      (select-keys [:ook/uri :label])
                      (assoc :codes matches))))))
       (remove nil?)))

(defn explain-facets [facets matching-observation-example dimensions codelists codes]
  (let [code-lookup (util/id-lookup codes)
        codelist-lookup (util/id-lookup codelists)]
    (->> facets
         (map (fn [facet]
                (let [facet-dimensions (filter (fn [d] (contains? (set (:dimensions facet))
                                                                  (:ook/uri d))) dimensions)
                      dimensions (explain-dimensions matching-observation-example
                                                     facet-dimensions
                                                     codelist-lookup
                                                     code-lookup)]
                  (when (seq dimensions)
                    {:name (:name facet)
                     :dimensions dimensions}))))
         (remove nil?))))

(defn explain-match
  "Replace matching observation example with per facet, per dimension, codelist and code summary"
  [datasets facets dimensions codelists codes]
  (for [{:keys [matching-observation-example] :as dataset} datasets]
    (let [facets (explain-facets facets matching-observation-example dimensions codelists codes)]
      (cond->
       (dissoc dataset :matching-observation-example)
        (seq facets) (assoc :facets facets)))))

(defn for-facets [selections opts]
  (let [codelist-uris (mapcat keys (vals selections))
        dimensions-lookup (components/codelist-to-dimensions-lookup codelist-uris opts)
        dimension-selections (facets/dimension-selections selections dimensions-lookup)
        observation-hits (find-observations dimension-selections opts)
        dataset-hits (datasets-from-observation-hits observation-hits)
        dataset-descriptions (for-cubes (map :ook/uri dataset-hits) opts)
        datasets (util/join-by dataset-hits dataset-descriptions :ook/uri :cube)
        code-uris (code-uris-from-observation-hits observation-hits)
        facets (facets/get-facets-for-selections selections opts)
        dimensions (components/get-components (mapcat :dimensions facets) opts)
        codelists (components/get-codelists codelist-uris opts)
        codes (codes/get-codes code-uris opts)]
    (explain-match datasets facets dimensions codelists codes)))

(defn total-count [{:keys [elastic/endpoint]}]
  (-> (esu/get-connection endpoint)
      (esd/count "dataset" "_doc")
      :count))

(comment
  (def conn (esu/get-connection "http://localhost:9200")))
