(ns ook.reframe.codes.search.db
  (:require
    [ook.reframe.codes.db.disclosure :as disclosure]))

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
