(ns ook.reframe.db
  (:require [ook.params.util :as pu]
            [reitit.core :as rt]
            [ook.reframe.router :as router]))

;; (defn code-selection->list [db]
;;   (some->> (:ui.codes/selection db)
;;            (filter (fn [[_k v]] v))
;;            (map first)))

;; (defn ->query-params [db]
;;   (cond-> {:q (:ui.codes/query db)}
;;     (seq (:ui.codes/selection db)) (merge {:facet (code-selection->list db)})))

(def initial-db
  {:app/current-route (rt/map->Match {:template "/" :path "/" :data router/home-route-data})
   :results.datasets/data []})

(defn filters->query-params [db]
  {:facet (->> (:facets/applied db)
               (map (fn [[name selection]]
                      (cons name selection)))
               (map pu/encode-facet))})
