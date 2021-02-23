(ns ook.search.elastic
  (:require [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [ook.search.db :as db]
            [integrant.core :as ig]))

(defn- parse-elastic-response [query response]
  {:result/query query
   :result/count (->> response :hits :total :value)
   :result/data (->> response :hits :hits
                     (map (fn [result]
                            {:id (-> result :_id)
                             :label (-> result :_source :label)})))})

(defn- es-search [query {:keys [elastic/endpoint]}]
  (let [conn (esr/connect endpoint {:content-type :json})
        response (esd/search conn "code" "_doc"
                             {:query {:match {:label query}}
                              :size 10000})]
    (parse-elastic-response query response)))

(defrecord Elasticsearch [opts]
  db/SearchBackend

  (get-codes [_ query]
    (es-search query opts)))

(defmethod ig/init-key :ook.search.elastic/db [_ opts]
  (->Elasticsearch opts))
