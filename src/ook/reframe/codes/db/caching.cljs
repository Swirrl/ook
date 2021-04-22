(ns ook.reframe.codes.db.caching
  (:require
   [ook.util :as u]
   [ook.reframe.db :as db]))

(defn cache-codelist [db facet-name codelists]
  (let [indexed-codelists (u/id-lookup codelists)]
    (assoc-in db [:facets/config facet-name :codelists] indexed-codelists)))

(defn cache-code-tree
  "Update the right codelist nested inside the facet config to store its children"
  [db facet-name codelist-uri children]
  (assoc-in db [:facets/config facet-name :codelists codelist-uri :children] children))
