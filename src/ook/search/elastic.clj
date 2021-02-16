(ns ook.search.elastic
  (:require [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]))

(defn- parse-elastic-response [query response]
  {:query query
   :count (->> response :hits :total :value)
   :results (->> response :hits :hits (map #(assoc (:_source %) :id (:_id %))))})

(defn query [es-endpoint query]
  (let [conn (esr/connect es-endpoint {:content-type :json})
        response (esd/search conn "code" "_doc"
                             :query {:match {:label query}}
                             :size 10000)]
    (parse-elastic-response query response)))
