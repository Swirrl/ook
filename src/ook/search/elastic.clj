(ns ook.search.elastic
  (:require [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [ook.search.db :as db]
            [integrant.core :as ig]
            [clojure.string :as str]))

(defn- get-connection [endpoint]
  (esr/connect endpoint {:content-type :json}))

(defn- parse-codes-response [response]
  (->> response :hits :hits
       (map (fn [result]
              {:id (-> result :_id)
               :scheme (-> result :_source :scheme)
               :label (-> result :_source :label)}))))

(defn- es-search [query {:keys [elastic/endpoint]}]
  (let [conn (get-connection endpoint)
        response (esd/search conn "code" "_doc"
                             {:query {:match {:label query}}
                              :size 10000})]
    (parse-codes-response response)))

(defn- workaround-hack
  "This is a temporary workaround to move forward with a snapshot db
  that has some issues with the data"
  [id]
  (str/replace id "dimension:" "def/dimension/"))

(defn- index-by-codelist [results]
  (->> results
       (mapcat (fn [hit]
                 (let [scheme (-> hit :_source :codelist)
                       dimension (-> hit :_id workaround-hack)]
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

(defn- get-datasets [conn codes dimensions]
  (->> (get-query-terms codes dimensions)
       (map (fn [[k v]]
              (->> (esd/search conn "observation" "_doc"
                               {:size 0
                                :query {:term {k v}}
                                :aggregations {:observation-count {:terms {:field "qb:dataSet.@id"}}}}))))
       (mapcat (fn [result]
                 (-> result :aggregations :observation-count :buckets)))
       (map (fn [{:keys [key doc_count]}]
              {:dataset key :matching-observations doc_count}))))

(defn- es-find-datasets [codes {:keys [elastic/endpoint]}]
  (let [conn (get-connection endpoint)
        dimensions (get-dimensions conn codes)]
    (if (seq dimensions)
      (get-datasets conn codes dimensions)
      [])))

(defrecord Elasticsearch [opts]
  db/SearchBackend

  (get-codes [_ query]
    (es-search query opts))

  (get-datasets [_ filters]
    (es-find-datasets filters opts)))

(defmethod ig/init-key :ook.search.elastic/db [_ opts]
  (->Elasticsearch opts))
