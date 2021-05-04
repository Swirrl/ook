(ns ook.reframe.facets.db
  (:require
   [ook.reframe.codes.db.disclosure :as disclosure]))

(defn get-codelists [db facet-name]
  (when-let [codelists (some-> db :facets/config (get facet-name) :codelists)]
    (if (empty? codelists)
      codelists
      (vals codelists))))

(defn- get-applied-selection [db facet-name]
  (get-in db [:facets/applied facet-name]))

(defn set-applied-selection-and-disclosure [db facet-name]
  (let [applied-selection (get-applied-selection db facet-name)
        base-ui-state {:name facet-name}]
    (if applied-selection
      (-> base-ui-state
          (assoc :selection applied-selection)
          (update :expanded disclosure/expand-all-selected-codes db applied-selection facet-name))
      base-ui-state)))

(defn get-dimensions [db facet-name]
  (-> db :facets/config (get facet-name) :dimensions))
