(ns ook.params.parse)

(defn get-query [{:keys [query-params]}]
  (get query-params "q"))
