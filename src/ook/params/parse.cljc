(ns ook.params.parse
  (:require
   [ook.util :as u]
   [clojure.string :as str]))

(defn get-query [{:keys [query-params]}]
  (get query-params "q"))

;; (defn parse-filters [{:keys [query-params]}]
;;   (let [param (get query-params "facet")]
;;     (when (seq param)
;;       (some->> param
;;                u/box
;;                (map #(str/split % #","))
;;                (map (fn [[dim val]] {:value val :dimension dim}))))))

(defn parse-named-facets
  "Parse query param facet tuples into a map of facet-name [codelists] for
  use in db queries, referred to as 'selection' elsewhere"
  [{:keys [query-params]}]
  (let [facets [(get query-params "facet")]]
    (reduce (fn [result [facet-name & codelists]]
              (assoc result facet-name codelists))
            {}
            facets)))
