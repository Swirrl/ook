(ns ook.search.elastic.components
  (:require [ook.search.elastic.util :as esu]
            [clojurewerkz.elastisch.query :as q]
            [ook.util :as u]
            [clojurewerkz.elastisch.rest.document :as esd]))

(defn get-components [uris {:keys [elastic/endpoint]}]
  (let [conn (esu/get-connection endpoint)
        uris (u/box uris)]
    (->> (esd/search conn "component" "_doc"
                     {:query (q/ids "_doc" uris)})
         :hits :hits (map :_source))))

(defn components->codelists [uris opts]
  (->> opts (get-components uris) (map :codelist) (remove nil?)))
