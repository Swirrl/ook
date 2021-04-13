(ns ook.reframe.db.selection)

(defn- codelist? [option]
  (nil? (:scheme option)))

(defn- add-to-selection [db {:keys [ook/uri] :as option}]
  (if (codelist? option)
    (assoc-in db [:ui.facets/current :selection uri] nil)
    (update-in db [:ui.facets/current :selection (:scheme option)]
               (fnil conj #{}) uri)))

(defn- remove-from-selection [db {:keys [ook/uri] :as option}]
  (if (codelist? option)
    (update-in db [:ui.facets/current :selection] dissoc uri)
    (update-in db [:ui.facets/current :selection (:scheme option)] disj uri)))

(defn option-selected? [db {:keys [ook/uri scheme]}]
  (let [selection (-> db :ui.facets/current :selection)]
    (if scheme
      (-> selection (get scheme) (get uri) boolean)
      (and (contains? selection uri) (nil? (get selection uri))))))

(defn toggle [db option]
  (if (option-selected? db option)
    (remove-from-selection db option)
    (add-to-selection db option)))
