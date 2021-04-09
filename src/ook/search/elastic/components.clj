(ns ook.search.elastic.components
  (:require [ook.search.elastic.util :as esu]
            [clojurewerkz.elastisch.query :as q]
            [clojure.set :as set]
            [ook.util :as u]
            [clojurewerkz.elastisch.rest.document :as esd]))

(defn get-components
  "Find components using their URIs."
  [uris {:keys [elastic/endpoint]}]
  (let [conn (esu/get-connection endpoint)
        uris (u/box uris)]
    (->> (esd/search conn "component" "_doc"
                     {:query (q/ids "_doc" uris)
                      :size (count uris)})
         :hits :hits
         (map :_source)
         (map esu/normalize-keys))))

(defn components->codelists [uris opts]
  (->> opts
       (get-components uris)
       (map :codelist)
       (remove nil?)
       distinct))

(defn- get-components-for-codelists
  "Find components using codelist URIs."
  [codelist-uris {:keys [elastic/endpoint] as :opts}]
  (let [conn (esu/get-connection endpoint)]
    (->>
     (esd/search conn "component" "_doc"
                 {:query
                  {:nested
                   {:path "codelist"
                    :query {:terms {"codelist.@id" codelist-uris}}}}})
     :hits :hits
     (map :_source)
     (map esu/normalize-keys))))

(defn codelist-to-dimensions-lookup
  "A lookup from each of the given codelist-uris to a vector of component-uris"
  [codelist-uris opts]
  (->>
   (get-components-for-codelists codelist-uris opts)
   (map (fn [component]
          (let [component-uri (:ook/uri component)
                codelist-uri (get-in component [:codelist :ook/uri])]
            {codelist-uri [component-uri]})))
   (apply merge-with concat)))

(defn get-codelists
  "Find codelists using their URIs."
  [uris opts]
  (->>
   (get-components-for-codelists uris opts)
   (map :codelist)))
