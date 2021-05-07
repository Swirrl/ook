(ns ook.reframe.codes.search.db
  (:require
   [ook.reframe.codes.db.disclosure :as disclosure]
   [ook.reframe.facets.db :as facets]))

(defn code-result->selection [result]
  (->> result
       (group-by :scheme)
       (map (fn [[scheme codes]]
              [scheme (set (map :ook/uri codes))]))
       (into {})))

(defn set-selection-and-disclosure [db facet-name results]
  (let [selection (code-result->selection results)]
    (-> {:name facet-name}
        (assoc :selection selection)
        (update :expanded disclosure/expand-all-selected-codes db selection facet-name))))

(defn get-results [db]
  (some-> db :ui.facets/current :codes/search :results))

(defn- results->visible-uris [results]
  (->> results
       (map #(select-keys % [:ook/uri :scheme]))
       (mapcat vals)
       set))

(defn- filter-visible-uris [result-uris codelist]
  (let [code-tree (if (= :no-children (:children codelist)) [] (:children codelist))
        open-codes (disclosure/find-open-codes result-uris code-tree)
        visible-uris (set (concat result-uris open-codes))
        walk  (fn walk* [node]
                (let [children (:children node)]
                  (when (visible-uris (:ook/uri node))
                    (assoc node :children (->> children (map walk*) (remove nil?) seq)))))]
    (walk codelist)))

(defn filter-to-search-results [codelists search-results]
  (let [result-uris (results->visible-uris search-results)]
    (->> codelists
         (map (partial filter-visible-uris result-uris))
         (remove nil?)
         (sort-by :ook/uri))))

(defn selectable-results [db]
  (->> db get-results (filter :used)))
