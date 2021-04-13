(ns ook.reframe.db.selection
  (:require [ook.reframe.db :as db]))

(defn- codelist? [option]
  (nil? (:scheme option)))

(defn add-codelist [db uri]
  (assoc-in db [:ui.facets/current :selection uri] nil))

(defn add-codes [db scheme code-uris]
  (update-in db [:ui.facets/current :selection scheme]
             #(apply (fnil conj #{}) % code-uris)))

(defn- add-to-selection [db {:keys [ook/uri] :as option}]
  (if (codelist? option)
    (add-codelist db uri)
    (add-codes db (:scheme option) [uri])))

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

(defn add-children [db {:keys [scheme ook/uri]}]
  (let [to-add (db/uri->child-uris db uri)]
    (add-codes db scheme to-add)))
