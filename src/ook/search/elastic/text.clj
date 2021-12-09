(ns ook.search.elastic.text
  (:require [clojure.string :as st]
            [clojurewerkz.elastisch.rest.document :as esd]
            [ook.search.elastic.util :as esu]
            [ook.search.elastic.components :as components]
            [ook.util :as u]))

(def size-limit 1000)

(defn add-snippet [codes conn cube]
  (let [matches (apply merge-with concat
                       (map (fn [code] {(:scheme code) [(select-keys code [:ook/uri :label])]}) codes))
        components (->> (esd/search conn "component" "_doc"
                                    {:query {:terms {"@id" (:component cube)}}
                                     :size size-limit})
                        :hits :hits
                        (map :_source)
                        (map esu/normalize-keys)
                        (map #(select-keys % [:ook/uri :label :codelist]))
                        (map (fn [component]
                               (update component :codelist (fn [codelist]
                                                             (assoc codelist :matches
                                                                    (matches (:ook/uri codelist))))))))
        snippet {:dimensions components}]
    (assoc cube :snippet snippet)))


(defn dataset-search
  "Find datasets by search code labels"
  [query {:keys [elastic/endpoint]}]
  (let [conn (esu/get-connection endpoint)
        codes (->> (esd/search conn "code" "_doc"
                               {:query {:bool {:must [{:match {:label {:query query
                                                                       :analyzer "std_english"}}}
                                                      {:term {:used "true"}}]}}
                                :size size-limit})
                   :hits :hits
                   (map :_source)
                   (map esu/normalize-keys))
        codelist-uris (distinct (mapcat (comp u/box :scheme) codes))
        components (->> (esd/search conn "component" "_doc"
                                    {:query
                                     {:nested
                                      {:path "codelist"
                                       :query {:terms {"codelist.@id" codelist-uris}}}}
                                     :size size-limit})
                        :hits :hits
                        (map :_source)
                        (map esu/normalize-keys))
        component-uris (distinct (map :ook/uri components))
        cubes (->> (esd/search conn "dataset" "_doc"
                               {:query {:terms {:component component-uris}}
                                :size size-limit})
                   :hits :hits
                   (map :_source)
                   (map esu/normalize-keys))]
    (map (partial add-snippet codes conn) cubes)))

(comment
  (let [opts {:elastic/endpoint "http://localhost:9200"}]
    (dataset-search "of" opts))
  )
