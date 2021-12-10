(ns ook.params.util
  (:require [reitit.impl :as ri]
            [clojure.string :as str]
            [swirrl.qb-filters.dsl :as dsl]))

(def base-uri "http://gss-data.org.uk/")
(def pmd-uri "https://beta.gss-data.org.uk/")

(defn- to-sets-of-codes [result {dim-uri :ook/uri codes :codes}]
  (if codes
    (let [code-uris (map :ook/uri codes)]
      (assoc result dim-uri (set code-uris)))
    result))

(defn absolute-uri [uri]
  (if (str/starts-with? uri "http")
    uri
    (str base-uri uri)))

(defn- absolutize-uris [[dim vals]]
  [(absolute-uri dim) (->> vals (map absolute-uri) set sort)])

(defn- encode-pmd-style [data]
  (->> data
       (map (fn [[dim vals]]
              [dim
               (mapv (fn [val] [:select [:individual val]]) vals)]))
       dsl/serialize))

(defn- remove-matches-where-codelist-applied
  "If the user specifies a codelist then the matching codes are just examples not criteria.
  This method removes these so that they don't feature in the PMD link."
  [applied-facets dataset-facets]
  (let [applied-codelist? (->> applied-facets
                               (mapcat val)
                               (filter (fn [[_codelist codes]] (empty? codes)))
                               (map key)
                               set)]
    (map (fn [facet]
           (update facet
                   :dimensions
                   (partial map (fn [dimension]
                                  (update
                                   dimension
                                   :codes
                                   (partial remove
                                            (fn [code]
                                              (some applied-codelist? (map :ook/uri (:scheme code)))))))))) dataset-facets)))

(defn encode-filter-facets
  "Encodes filter facets in the form [dimension value] the same way
  as PMD so that they can be used to construct a link to a cube in pmd"
  [dataset-facets applied-facets]
  (->> dataset-facets
       (remove-matches-where-codelist-applied applied-facets)
       (mapcat :dimensions)
       (reduce to-sets-of-codes {})
       (map absolutize-uris)
       (into {})
       encode-pmd-style))

;; with facets...
(defn pmd-link-from-facets [id dataset-facets applied-facets]
  (let [filter-facets (encode-filter-facets dataset-facets applied-facets)
        query-string (str (ri/query-string (cond-> {:uri (str base-uri id)}
                                             (seq filter-facets) (assoc :apply-filters true
                                                                        :qb-filters filter-facets))))]
    (str pmd-uri "cube/explore?" query-string)))

;; without facets...
(defn pmd-link-from-dataset [dataset]
  (let [uri (:ook/uri dataset)
        filters (->> dataset
                     :component
                     (filter #(contains? % :matches))
                     (map (fn [{:keys [ook/uri matches]}]
                            [uri (map :ook/uri matches)])))
        params (cond-> {:uri (absolute-uri uri)}
                 (seq filters)
                 (assoc
                  :apply-filters true
                  :qb-filters (encode-pmd-style (map absolutize-uris filters))))]
    (str pmd-uri "cube/explore?" (str (ri/query-string params)))))
