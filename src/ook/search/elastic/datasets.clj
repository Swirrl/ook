(ns ook.search.elastic.datasets
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojure.set :as set]
   [meta-merge.core :as mm]
   [clojurewerkz.elastisch.query :as q]
   [clojurewerkz.elastisch.rest.document :as esd]
   [ook.search.elastic.util :as esu]
   [ook.util :as u]))

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
