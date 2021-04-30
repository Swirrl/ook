(ns ook.reframe.facets.db
  (:require
    [ook.reframe.codes.db.disclosure :as disclosure]))

(defn set-current-facet [db facet]
  (let [status (if (empty? (:codelists facet)) :success/empty :success/ready)]
    (-> db
        (assoc :ui.facets/current facet)
        (assoc :ui.facets.current/status status))))

(defn- get-applied-selection [db facet-name]
  (get-in db [:facets/applied facet-name]))

(defn set-applied-selection-and-disclosure [db {:keys [name] :as facet}]
  (if-let [applied-selection (get-applied-selection db name)]
    (-> facet
        (assoc :selection applied-selection)
        (update :expanded disclosure/add-all-open-codes db applied-selection name))
    facet))
