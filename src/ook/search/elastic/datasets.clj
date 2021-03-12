(ns ook.search.elastic.datasets
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojurewerkz.elastisch.query :as q]
   [clojurewerkz.elastisch.rest.document :as esd]
   [ook.search.elastic.util :as esu]
   [ook.util :as u]))

(defn- index-by-codelist [results]
  (->> results
       (mapcat (fn [hit]
                 (let [scheme (-> hit :_source :codelist)
                       dimension (-> hit :_id)]
                   (if (coll? scheme)
                     ;; right now some component ids correspond to multiple codelists -- is this actually allowed?
                     (map #(vector % dimension) scheme)
                     [[scheme dimension]]))))
       (into {})))

(defn- get-dimensions [conn codes]
  (when (seq codes)
    (let [schemes (->> codes (map :scheme) distinct)]
      (->> (esd/search conn "component" "_doc"
                       {:query {:terms {:codelist schemes}}})
           :hits :hits
           index-by-codelist))))

(defn- get-query-terms [codes dimensions]
  (->> codes
       (map #(vector (-> dimensions (get (:scheme %)) (str ".@id")) (:id %)))
       (into {})))


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

(defn- get-datasets [conn codes dimensions]
  (let [matches (->> (get-query-terms codes dimensions)
                     (map (fn [[k v]]
                            (->> (esd/search conn "observation" "_doc"
                                             {:size 0
                                              :query {:term {k v}}
                                              :aggregations {:observation-count {:terms {:field "qb:dataSet.@id"}}}}))))
                     (mapcat (fn [result]
                               (-> result :aggregations :observation-count :buckets)))
                     (map (fn [{:keys [key doc_count]}]
                            {:cube key :matching-observations doc_count})))
        more-ds-info (cubes->datasets conn (map :cube matches))]
    (u/mjoin matches more-ds-info :cube)))

(defn apply-filter [codes {:keys [elastic/endpoint]}]
  (let [conn (esu/get-connection endpoint)
        dimensions (get-dimensions conn codes)]
    (if (seq dimensions)
      (get-datasets conn codes dimensions)
      [])))

(defn all [{:keys [elastic/endpoint]}]
  (-> (esu/get-connection endpoint)
      (esd/search "dataset" "_doc" {:query (q/match-all)})
      clean-datasets-result))

(comment
  (def conn (esu/get-connection "http://localhost:9200"))
  )
