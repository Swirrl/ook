(ns ook.reframe.db)

;; (defn code-selection->list [db]
;;   (some->> (:ui.codes/selection db)
;;            (filter (fn [[_k v]] v))
;;            (map first)))

;; (defn ->query-params [db]
;;   (cond-> {:q (:ui.codes/query db)}
;;     (seq (:ui.codes/selection db)) (merge {:facet (code-selection->list db)})))

(defn filters->query-params [db]
  {:facet (map (fn [[name selection]]
                 (cons name selection))
               (:facets/applied db))})
