(ns ook.reframe.codes.db.selection
  (:require
    [meta-merge.core :as mm]
    [clojure.set :as set]
    [ook.reframe.codes.db.disclosure :as disclosure]))

(defn- codelist? [option]
  (nil? (:scheme option)))

(defn add-codelist [facet uri]
  (assoc-in facet [:selection uri] nil))

(defn- add-codes
  [facet scheme code-uris]
  (update-in facet [:selection scheme] #(apply (fnil conj #{}) % code-uris)))

(defn- add-to-selection [facet {:keys [ook/uri] :as option}]
  (if (codelist? option)
    (add-codelist facet uri)
    (add-codes facet (:scheme option) [uri])))

(defn- dissoc-empty-scheme [selection scheme]
  (if (-> selection (get scheme) empty?)
    (dissoc selection scheme)
    selection))

(defn- remove-code [facet {:keys [ook/uri scheme]}]
  (-> facet
      (update-in [:selection scheme] disj uri)
      (update :selection dissoc-empty-scheme scheme)))

(defn- remove-from-selection [facet {:keys [ook/uri] :as option}]
  (if (codelist? option)
    (update facet :selection dissoc uri)
    (remove-code facet option)))

(defn option-selected? [{:keys [selection]} {:keys [ook/uri scheme]}]
  (if scheme
    (-> selection (get scheme) (get uri) boolean)
    (and (contains? selection uri) (nil? (get selection uri)))))

(defn indeterminate? [selected-uris option]
  (let [walk (fn walk* [node]
               (cons (:ook/uri node)
                     (when-let [children (:children node)]
                       (when-not (= :no-children children)
                         (mapcat walk* children)))))
        all-child-uris (set (walk option))]
    (boolean (seq (set/intersection selected-uris all-child-uris)))))

(defn toggle [facet option]
  (if (option-selected? facet option)
    (remove-from-selection facet option)
    (add-to-selection facet option)))

(defn used-child-uris [{:keys [children]}]
  (->> children (filter :used) (map :ook/uri) set))

(defn add-children [facet-ui {:keys [scheme] :as code}]
  (let [used-child-uris (used-child-uris code)]
    (if (seq used-child-uris)
      (add-codes facet-ui scheme used-child-uris)
      facet-ui)))

(defn remove-children [facet {:keys [scheme] :as code}]
  (update-in facet [:selection scheme]
             #(apply (fnil disj #{}) % (used-child-uris code))))

(defn remove-codes [current-selection codes]
  (reduce (fn [selection {:keys [scheme ook/uri]}]
            (update-in selection [scheme] disj uri))
          current-selection
          codes))

(defn all-selected-uris [db]
  (let [selection (-> db :ui.facets/current :selection)]
    (set
      (reduce (fn [uris [scheme code-uris]]
                (if (seq code-uris)
                  (concat uris code-uris)
                  (conj uris scheme)))
              []
              selection))))
