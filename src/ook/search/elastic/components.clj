(ns ook.search.elastic.components
  (:require [ook.search.elastic.util :as esu]
            [clojurewerkz.elastisch.rest.document :as esd]))

(defn get-components [uris {:keys [elastic/endpoint] :as opts}]
  (let [conn (esu/get-connection endpoint)
        uris (if (seq? uris) uris (vector uris))]
    (->> (esd/search conn "component" "_doc"
                     {:query {:terms {:_id uris}}})
         :hits :hits (map :_source))))
