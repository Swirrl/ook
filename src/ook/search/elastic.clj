(ns ook.search.elastic
  (:require [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]))

(defn- parse-elastic-response [query response]
  {:query query
   :count (->> response :hits :total :value)
   :results (->> response :hits :hits (map #(assoc (:_source %) :id (:_id %))))})

(defn query [query]
  ;; TODO: this should be pass as integrant config
  (let [conn (esr/connect "http://127.0.0.1:9200" {:content-type :json})
        response (esd/search conn "code" "_doc" :query {:match {:label query}})]
    (parse-elastic-response query response)))
