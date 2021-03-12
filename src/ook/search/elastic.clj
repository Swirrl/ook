(ns ook.search.elastic
  (:require [clojurewerkz.elastisch.rest.document :as esd]
            [ook.search.db :as db]
            [ook.search.elastic.datasets :as ds]
            [ook.search.elastic.util :as u]
            [integrant.core :as ig]))

(defn- parse-codes-response [response]
  (->> response :hits :hits
       (map (fn [result]
              {:id (-> result :_id)
               :scheme (-> result :_source :scheme)
               :label (-> result :_source :label)}))))

(defn- es-search [query {:keys [elastic/endpoint]}]
  (let [conn (u/get-connection endpoint)
        response (esd/search conn "code" "_doc"
                             {:query {:match {:label query}}
                              :size 10000})]
    (parse-codes-response response)))

(defrecord Elasticsearch [opts]
  db/SearchBackend

  (get-codes [_ query]
    (es-search query opts))

  (get-datasets [_ filters]
    (ds/apply-filter filters opts))

  (all-datasets [_]
    (ds/all opts)))

(defmethod ig/init-key :ook.search.elastic/db [_ opts]
  (->Elasticsearch opts))
