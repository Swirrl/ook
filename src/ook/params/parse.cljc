(ns ook.params.parse
  (:require
   [ook.util :as u]
   [ook.concerns.transit :as t]))

(defn serialize-filter-state [filter-state]
  (t/write-string filter-state))

(defn deserialize-filter-state [filter-state]
  (t/read-string filter-state))

(defn get-facets
  [{:keys [query-params]}]
  (when (seq query-params)
    (-> query-params (get "filters") deserialize-filter-state)))

(defn get-dimensions [{:keys [query-params]}]
  (when (seq query-params)
    (-> query-params (get "dimension") u/box)))

(defn get-codelist [{:keys [query-params]}]
  (when (seq query-params)
    (get query-params "codelist")))

(defn get-search-params [{:keys [query-params]}]
  {:search-term (query-params "search-term")
   :codelists (u/box (query-params "codelists"))})
