(ns ook.search.elastic.datasets
  (:require
   [clojure.set :as set]
   [meta-merge.core :as mm]
   [clojurewerkz.elastisch.query :as q]
   [clojurewerkz.elastisch.rest.document :as esd]
   [ook.search.elastic.util :as esu]
   [ook.util :as u]
   [ook.search.elastic.facets :as facets]
   [ook.search.elastic.components :as components]))

(defn- flatten-description-lang-strings [m]
  (-> m
      (assoc :description (-> m :dcterms:description :value))
      (dissoc :dcterms:description)))

(defn- clean-datasets-result [result]
  (->> result :hits :hits
       (map :_source)
       (map esu/normalize-keys)
       (map flatten-description-lang-strings)))

;; (defn- cubes->datasets [conn cubes]
;;   (-> conn
;;       (esd/search "dataset" "_doc" {:query {:terms {:cube cubes}}})
;;       clean-datasets-result))

;; (defn- datasets-for-facet [conn {:keys [dimension value] :as facet}]
;;   (->> (esd/search conn "observation" "_doc"
;;                    {:size 0
;;                     :query {:term {(str dimension ".@id") value}}
;;                     :aggregations {:observation-count
;;                                    {:terms
;;                                     {:field "qb:dataSet.@id"}}}})
;;        :aggregations :observation-count :buckets
;;        (map #(set/rename-keys % {:key :cube
;;                                  :doc_count :matching-observations}))
;;        (map #(assoc % :filter-facets [facet]))))

;; (defn- get-datasets [conn facets]
;;   (let [matches (->> facets
;;                      ;; get all datasets that match a given facet
;;                      (mapcat (partial datasets-for-facet conn))
;;                      (group-by :cube)
;;                      ;; group all filter facets that found this dataset into one list
;;                      (map (fn [[_ datasets]] (reduce mm/meta-merge datasets))))
;;         more-ds-info (cubes->datasets conn (map :cube matches))]
;;     (u/mjoin matches more-ds-info :cube)))

;; (defn apply-filter [facets {:keys [elastic/endpoint]}]
;;   (let [conn (esu/get-connection endpoint)]
;;     (get-datasets conn facets)))

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

(defn for-facets [selections opts]
  (let [facets (facets/get-facets opts)
        faceted-dimensions (distinct (mapcat :dimensions facets))
        components (components/get-components faceted-dimensions opts)
        datasets (for-components faceted-dimensions opts)]
    (->>
     (facets/apply-facets datasets components facets selections)
     (map esu/normalize-keys)
     (map flatten-description-lang-strings))))

(comment
  (def conn (esu/get-connection "http://localhost:9200")))
