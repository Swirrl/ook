(ns ook.params.parse
  (:require
   [ook.util :as u]
   [cemerick.url :as url]
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

(defn parse-named-facets [facets]
  (->> facets
       u/box
       (map #(str/split % #","))
       (map #(map url/url-decode %))
       (reduce (fn [result [facet-name & codelists]]
                 (assoc result facet-name codelists))
               {})))

(defn get-facets
  "Parse query param facet tuples into a map of facet-name [codelists] for
  use in db queries, referred to as 'selection' elsewhere"
  [{:keys [query-params]}]
  (when (seq query-params)
    (-> query-params (get "facet") parse-named-facets)))

(defn get-codelists
  [{:keys [query-params]}]
  (when (seq query-params)
    (-> query-params u/box (get "codelist"))))
