(ns ook.reframe.codes.search.db)

(defn code-result->selection [result]
  (->> result
       (group-by :scheme)
       (map (fn [[scheme codes]]
              [scheme (set (map :ook/uri codes))]))
       (into {})))
