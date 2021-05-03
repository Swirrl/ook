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

(def size-limit 500)

(defn- clean-datasets-result [result]
  (->> result :hits :hits
       (map :_source)
       (map esu/normalize-keys)))

(defn- select-description [dataset]
  (cond-> dataset
    (:comment dataset) (dissoc dataset :description)))

(defn- select-relevant-fields [dataset]
  (-> dataset
      select-description
      (select-keys [:label :description :comment :publisher
                    :ook/uri :matching-observation-count :facets])
      (update :publisher select-keys [:altlabel])))

(defn all [{:keys [elastic/endpoint]}]
  (->> (esd/search (esu/get-connection endpoint)
                   "dataset" "_doc" {:query (q/match-all)
                                     :size size-limit})
       clean-datasets-result
       (map select-relevant-fields)))

(defn for-components [components {:keys [elastic/endpoint] :as opts}]
  (let [conn (esu/get-connection endpoint)]
    (->> (esd/search conn "dataset" "_doc"
                     {:query {:terms {:component components}}
                      :size size-limit})
         :hits :hits (map :_source))))

(defn- for-cubes
  "Find datasets for cube URIs"
  [cube-uris {:keys [elastic/endpoint]}]
  (let [conn (esu/get-connection endpoint)
        uris (util/box cube-uris)]
    (->> (esd/search conn "dataset" "_doc"
                     {:query {:terms {:cube uris}}
                      :size (count uris)})
         clean-datasets-result)))

(defn- append-id-to-dimensions
  "append .@id to reach into nested dimval docs"
  [dimension-selections]
  (let [append-id (fn [x] (str x ".@id"))]
    (into {} (for [[f s] dimension-selections]
               [f (into {} (for [[d vs] s]
                             [(append-id d) vs]))]))))

(defn- observation-query
  "Creates a query for finding observations with selected dimensions and dimension-values.

  Within a facet, dimensions are combined with 'should' i.e. OR. Facets themselves are combined with 'must' i.e. AND.

  Provides counts by dataset and identifies (and counts observations for) the top three codes by dimension."
  [dimension-selections]
  (let [dimension-selections (append-id-to-dimensions dimension-selections)
        dimension-clause (fn [[dim codes]]
                           (if (empty? codes)
                             {:exists {:field dim}} ;; dimension (with no codes specified) ought to be present
                             {:terms {dim codes}})) ;; dimensions' values are one of the specified codes
        facet-criteria (into {} (map (fn [[facet criteria]]
                                       [facet (map dimension-clause criteria)]) dimension-selections))
        dimensions (->> dimension-selections (map (comp keys val)) flatten distinct)
        dimension-rollup (into {} (map (fn [dim] [dim {:terms {:field dim, :size 3}}]) ;; select the top 3 codes per dimension
                                       dimensions))]
    {:size 0
     :query {:bool {:must (map (fn [[facet criteria]] {:bool {:should criteria}}) facet-criteria)}}
     :aggregations {:datasets {:terms {:field "qb:dataSet.@id" :size size-limit} ;; roll-up dimensions within each dataset
                               :aggregations dimension-rollup}}}))

(defn- find-observations
  "Finds observations with given dimensions and dimension-values. Groups results by dataset."
  [dimension-selections {:keys [elastic/endpoint] :as opts}]
  (let [conn (esu/get-connection endpoint)]
    (esd/search conn "observation" "_doc"
                (observation-query dimension-selections))))

(defn datasets-from-observation-hits
  "Parses observation-query results to return a sequence of datasets."
  [observation-hits]
  (->> observation-hits :aggregations :datasets :buckets
       (map (fn [{:keys [key doc_count] :as dataset}]
                 (let [dimension-buckets (dissoc dataset :key :doc_count) ;; if we remove ES bookkeeping we should be left with a map from dim.@id to buckets
                       dimension-values (into {} (map (fn [[dim {:keys [buckets]}]] (when (not (empty? buckets))
                                                                                      [dim (map :key buckets)]))
                                                      dimension-buckets))]
                   {:ook/uri key
                    :matching-observation-count doc_count
                    :matching-dimension-values dimension-values})))))

(defn code-uris-from-datasets
  "Parses observation-query results to return a sequence of code-uris."
  [datasets]
  (->> datasets
       (map (comp vals :matching-dimension-values))
       flatten
       distinct))

(defn explain-dimensions [matching-dimension-values dimensions codelist-lookup code-lookup]
  (->> dimensions
       (map (fn [dimension]
              (let [code-uris (some-> matching-dimension-values
                                      (get (keyword (str (:ook/uri dimension) ".@id")))
                                      util/box)
                    matches (some->> code-uris
                                     (map code-lookup)
                                     (remove nil?)
                                     (map (fn [code] (update code :scheme #(->> % util/box (map codelist-lookup))))))]
                (when matches
                  (cond-> (select-keys dimension [:ook/uri :label])
                    (seq matches) (assoc :codes matches))))))
       (remove nil?)))

(defn explain-facets [facets matching-dimension-values dimensions codelists codes]
  (let [code-lookup (util/id-lookup codes)
        codelist-lookup (util/id-lookup codelists)]
    (->> facets
         (map (fn [facet]
                (let [facet-dimensions (filter (fn [d] (contains? (set (:dimensions facet))
                                                                  (:ook/uri d))) dimensions)
                      dimensions (explain-dimensions matching-dimension-values
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
  (for [{:keys [matching-dimension-values] :as dataset} datasets]
    (let [facets (explain-facets facets matching-dimension-values dimensions codelists codes)]
      (cond->
       (dissoc dataset :matching-dimension-values)
        (seq facets) (assoc :facets facets)))))

(defn for-facets [selections opts]
  (let [codelist-uris (mapcat keys (vals selections))
        dimensions-lookup (components/codelist-to-dimensions-lookup codelist-uris opts)
        dimension-selections (facets/dimension-selections selections dimensions-lookup)
        observation-hits (find-observations dimension-selections opts)
        dataset-hits (datasets-from-observation-hits observation-hits)
        dataset-descriptions (for-cubes (map :ook/uri dataset-hits) opts)
        datasets (util/join-by dataset-hits dataset-descriptions :ook/uri :cube)
        code-uris (code-uris-from-datasets datasets)
        facets (facets/get-facets-for-selections selections opts)
        dimensions (components/get-components (mapcat :dimensions facets) opts)
        codelists (components/get-codelists codelist-uris opts)
        codes (codes/get-codes code-uris opts)]
    (->> (explain-match datasets facets dimensions codelists codes)
         (map select-relevant-fields))))

(defn total-count [{:keys [elastic/endpoint]}]
  (-> (esu/get-connection endpoint)
      (esd/count "dataset" "_doc")
      :count))

(comment
  (def conn (esu/get-connection "http://localhost:9200")))
