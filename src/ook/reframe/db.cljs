(ns ook.reframe.db)

(def initial-state {})

(defn code-selection->list [db]
  (some->> (:ui.codes/selection db)
           (filter (fn [[_k v]] v))
           (map first)))

(defn ->query-params [db]
  (cond-> {:q (:ui.codes/query db)}
    (seq (:ui.codes/selection db)) (merge {:facet (code-selection->list db)})))
