(ns ook.reframe.codes.db.caching
  (:require
   [ook.util :as u]
   [ook.reframe.db :as db]))

(defn cache-codelist
  "This is an optimization to save requests. Dimensions are replaced with codelists
  in the front-end app state the first time a facet is selected in the ui."
  [db facet-name codelists]
  (let [indexed-codelists (u/id-lookup codelists)]
    (-> db
        (assoc-in [:facets/config facet-name :codelists] indexed-codelists)
        (update-in [:facets/config facet-name] dissoc :dimensions))))

(defn cache-code-tree
  "Update the right codelist nested inside the facet config to store its children"
  [db facet-name codelist-uri children]
  (assoc-in db [:facets/config facet-name :codelists codelist-uri :children] children))
