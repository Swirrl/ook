(ns ook.search.elastic.codes
  (:require
   [ook.util :as u]
   [clojurewerkz.elastisch.query :as q]
   [ook.search.elastic.util :as esu]
   [clojurewerkz.elastisch.rest.document :as esd]))

;; (defn- index-by-codelist [results]
;;   (->> results
;;        (map (fn [result]
;;               [(-> result :_source :codelist) {:dim/id (-> result :_id)
;;                                                :dim/label (-> result :_source :label)}]))
;;        (into {})))

;; (defn- get-dimensions [conn codes]
;;   (when (seq codes)
;;     (let [schemes (->> codes (map :codelist) distinct)]
;;       (-> conn
;;           (esd/search "component" "_doc" {:query {:terms {:codelist schemes}}})
;;           :hits :hits
;;           index-by-codelist))))

(defn get-codes* [conn uris]
  (->> (esd/search conn "code" "_doc"
                   {:query (q/ids "_doc" uris)
                    :size (count uris)})
       :hits :hits
       (map :_source)
       (map esu/normalize-keys)))

(defn get-codes
  "Find codes using their URIs."
  [uris {:keys [elastic/endpoint]}]
  (let [conn (esu/get-connection endpoint)
        uris (u/box uris)]
    (get-codes* conn uris)))


;; (defn- get-codes [conn query]
;;   (-> conn
;;       (esd/search "code" "_doc" {:query {:match {:label query}} :size 10000})
;;       :hits :hits
;;       build-codes))

;; (defn search [query {:keys [elastic/endpoint]}]
;;   (let [conn (esu/get-connection endpoint)
;;         codes (get-codes conn query)
;;         dimensions (get-dimensions conn codes)]
;;     (map (fn [code]
;;            (merge code (dimensions (:codelist code))))
;;          codes)))

(defn- build-codes
  "Map the codes from the elasticsearch result format to a more succinct format used internally.
  The scheme is passed in as an argument and not taken from (-> result :_source :scheme) because
  sometimes a code belongs to multiple schemes and that value is a collection.. It's included in
  the result in the first place so that each individual map contains all the information it
  needs to fetch its own children."
  [results scheme]
  (map (fn [result]
         {:scheme scheme
          :ook/uri (-> result :_id)
          :label (-> result :_source :label)
          :children (-> result :_source :narrower)})
       results))

(defn get-top-concepts [conn codelist-id]
  (-> conn
      (esd/search "code" "_doc"
                  {:size 500
                   :query
                   {:bool
                    {:must {:term {:scheme codelist-id}}
                     :must_not {:exists {:field :broader}}}}})
      :hits :hits
      (build-codes codelist-id)))

(declare find-narrower-concepts)

(defn- build-sub-tree [conn code-uris]
  (doall
   (map (fn [code]
          (if (:children code)
            (find-narrower-concepts conn code)
            code))
        (get-codes* conn code-uris))))

(defn- find-narrower-concepts [conn {:keys [children] :as concept}]
  (if (seq children)
    (assoc concept :children (build-sub-tree conn children))
    concept))

(defn build-concept-tree [codelist-id {:keys [elastic/endpoint]}]
  (if codelist-id
    (let [conn (esu/get-connection endpoint)]
      (doall
       (map (partial find-narrower-concepts conn)
            (get-top-concepts conn codelist-id))))
    []))
