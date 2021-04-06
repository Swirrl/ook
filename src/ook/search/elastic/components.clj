(ns ook.search.elastic.components
  (:require [ook.search.elastic.util :as esu]
            [clojurewerkz.elastisch.query :as q]
            [clojure.set :as set]
            [ook.util :as u]
            [clojurewerkz.elastisch.rest.document :as esd]))

(def max-hits 10000) ;; order of magnitude greater than we can expect for the prototype

(defn get-components [uris {:keys [elastic/endpoint]}]
  (let [conn (esu/get-connection endpoint)
        uris (u/box uris)]
    (->> (esd/search conn "component" "_doc"
                     {:query (q/ids "_doc" uris)
                      :size max-hits})
         :hits :hits
         (map :_source)
         (map esu/normalize-keys))))

(defn components->codelists [uris opts]
  (->> opts
       (get-components uris)
       (map :codelist)
       (remove nil?)))
