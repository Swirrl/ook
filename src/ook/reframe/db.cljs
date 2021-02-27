(ns ook.reframe.db)

(def initial-state
  "This could just be an empty map but is here to demonstrate all the state the
  front-end has access to"

  {:ui.codes/query ""})

(defn code-selection->list [db]
  (some->> (:ui.codes/selection db)
           (filter (fn [[_k v]] v))
           (map first)))

(defn ->query-params [db]
  (cond-> {:q (:ui.codes/query db)}
    (seq (:ui.codes/selection db)) (merge {:code (code-selection->list db)})))
