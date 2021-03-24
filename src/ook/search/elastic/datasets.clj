(ns ook.search.elastic.datasets
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojure.set :as set]
   [meta-merge.core :as mm]
   [clojurewerkz.elastisch.query :as q]
   [clojurewerkz.elastisch.rest.document :as esd]
   [ook.search.elastic.util :as esu]
   [ook.util :as u]
   [ook.search.elastic.facets :as facets]
   [ook.search.elastic.components :as components]))

(defn- normalize-keys [m]
  (let [remove-at (fn [[k v]]
                    (if (str/includes? (str k) "@")
                      [(keyword (str/replace k ":@" "")) v]
                      [k v]))]
    ;; only apply to maps
    (walk/postwalk (fn [x] (if (map? x) (into {} (map remove-at x)) x))
                   m)))

(defn- flatten-description-lang-strings [m]
  (-> m
      (assoc :description (-> m :dcterms:description :value))
      (dissoc :dcterms:description)))

(defn- clean-datasets-result [result]
  (->> result :hits :hits
       (map :_source)
       (map normalize-keys)
       (map flatten-description-lang-strings)))

(defn- cubes->datasets [conn cubes]
  (-> conn
      (esd/search "dataset" "_doc" {:query {:terms {:cube cubes}}})
      clean-datasets-result))

(defn- datasets-for-facet [conn {:keys [dimension value] :as facet}]
  (->> (esd/search conn "observation" "_doc"
                   {:size 0
                    :query {:term {(str dimension ".@id") value}}
                    :aggregations {:observation-count
                                   {:terms
                                    {:field "qb:dataSet.@id"}}}})
       :aggregations :observation-count :buckets
       (map #(set/rename-keys % {:key :cube
                                 :doc_count :matching-observations}))
       (map #(assoc % :filter-facets [facet]))))

(defn- get-datasets [conn facets]
  (let [matches (->> facets
                     ;; get all datasets that match a given facet
                     (mapcat (partial datasets-for-facet conn))
                     (group-by :cube)
                     ;; group all filter facets that found this dataset into one list
                     (map (fn [[_ datasets]] (reduce mm/meta-merge datasets))))
        more-ds-info (cubes->datasets conn (map :cube matches))]
    (u/mjoin matches more-ds-info :cube)))

(defn apply-filter [facets {:keys [elastic/endpoint]}]
  (let [conn (esu/get-connection endpoint)]
    (get-datasets conn facets)))

(defn all [{:keys [elastic/endpoint]}]
  (-> (esu/get-connection endpoint)
      (esd/search "dataset" "_doc" {:query (q/match-all)})
      clean-datasets-result))

(comment
  (def conn (esu/get-connection "http://localhost:9200"))
  )

(defn for-components [components {:keys [elastic/endpoint] :as opts}]
  (let [conn (esu/get-connection endpoint)]
    (->> (esd/search conn "dataset" "_doc"
                    {:query {:terms {:component components}}})
         :hits :hits (map :_source))))

(defn for-facets [facet-names opts]
  (let [facets (->> (facets/get-facets opts) ;; pass db instead of opts here?
                    ;; get only those facets that've been set
                    (filter #((set facet-names) (:name %))))
        facet-dimensions (distinct (mapcat :dimensions facets)) ;; for filtering
        components (components/get-components facet-dimensions opts)
        datasets (for-components facet-dimensions opts)]
    (reduce (fn [datasets facet]
              (let [all-dimensions (:dimensions facet)]
                (->> datasets
                     (map (fn [dataset]
                            (let [dataset-dimensions (:component dataset)
                                  matched-dimensions (set/intersection (set all-dimensions)
                                                                       (set dataset-dimensions))
                                  codelists (->> components
                                                 (filter #(matched-dimensions ((keyword "@id") %)))
                                                 (map :codelist)
                                                 distinct)]
                              (assoc-in dataset [:facet (:name facet)] codelists))))
                     (map normalize-keys)
                     (map flatten-description-lang-strings))))
            datasets
            facets)))
