(ns ook.search.elastic.codes
  (:require
   [ook.search.elastic.util :as esu]
   [clojurewerkz.elastisch.rest.document :as esd]))

(defn- index-by-codelist [results]
  (->> results
       (map (fn [result]
                 [(-> result :_source :codelist) {:dim/id (-> result :_id)
                                                  :dim/label (-> result :_source :label)}]))
       (into {})))

(defn- get-dimensions [conn codes]
  (when (seq codes)
    (let [schemes (->> codes (map :codelist) distinct)]
      (-> conn
          (esd/search "component" "_doc" {:query {:terms {:codelist schemes}}})
          :hits :hits
          index-by-codelist))))

(defn- build-codes [results]
  (map (fn [result]
         {:codelist (-> result :_source :scheme)
          :code/id (-> result :_id)
          :code/label (-> result :_source :label)})
       results))

(defn- get-codes [conn query]
  (-> conn
      (esd/search "code" "_doc" {:query {:match {:label query}} :size 10000})
      :hits :hits
      build-codes))

(defn search [query {:keys [elastic/endpoint]}]
  (let [conn (esu/get-connection endpoint)
        codes (get-codes conn query)
        dimensions (get-dimensions conn codes)]
    (map (fn [code]
           (merge code (dimensions (:codelist code))))
         codes)))
