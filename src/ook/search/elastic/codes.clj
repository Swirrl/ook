(ns ook.search.elastic.codes
  (:require
   [ook.util :as u]
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

(defn- get-scheme [result]
  ;; TODO:: This is a hack to make things work right now, just (deterministically) select the first scheme
  ;; almost certainly not the right way to handle a concept that belongs to multiple schemes
  (let [scheme (-> result :_source :scheme)]
    (if (coll? scheme)
      (->> scheme sort first)
      scheme)))

(defn- build-codes [results]
  (map (fn [result]
         {:scheme (get-scheme result)
          :ook/uri (-> result :_id)
          :label (-> result :_source :label)})
       results))

(defn get-top-concepts [conn codelist-id]
  (-> conn
      (esd/search "code" "_doc"
                  {:query
                   {:bool
                    {:must {:term {:scheme codelist-id}}
                     :must_not {:exists {:field :broader}}}}})
      :hits :hits
      build-codes))

(defn get-children [conn {:keys [ook/uri scheme]}]
  (-> conn
      (esd/search "code" "_doc"
                  {:query
                   {:bool
                    {:must [{:term {:scheme scheme}}
                            {:term {:broader uri}}]}}})
      :hits :hits
      build-codes))

(declare find-narrower-concepts)

(defn- build-sub-tree [conn concepts]
  (doall
    (map (fn [concept]
           (if (:children concept)
             (find-narrower-concepts conn concept)
             concept))
         concepts)))

(defn- find-narrower-concepts [conn concept]
  (let [children (get-children conn concept)]
    (if (seq children)
      (assoc concept :children (build-sub-tree conn children))
      concept)))

(defn build-tree [conn codelist-id]
  (doall
   (map (partial find-narrower-concepts conn)
        (get-top-concepts conn codelist-id))))

(defn build-code-trees [codelist-ids {:keys [elastic/endpoint]}]
  (when codelist-ids
    (let [conn (esu/get-connection endpoint)]
      (doall (mapcat (partial build-tree conn) (u/box codelist-ids))))))
