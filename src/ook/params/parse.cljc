(ns ook.params.parse
  (:require
   [ook.util :as u]
   [ook.concerns.transit :as t]))

(defn deserialize-filter-state [filter-state]
  (t/read-string filter-state))

(defn get-facets
  "Parse query param facet tuples into a map of facet-name [codelists] for
  use in db queries, referred to as 'selection' elsewhere"
  [{:keys [query-params]}]
  (when (seq query-params)
    (-> query-params (get "filters") deserialize-filter-state)))

(defn get-dimensions [{:keys [query-params]}]
  (when (seq query-params)
    (-> query-params (get "dimension") u/box)))

(defn get-codelist [{:keys [query-params]}]
  (when (seq query-params)
    (get query-params "codelist")))
