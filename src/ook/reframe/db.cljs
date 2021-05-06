(ns ook.reframe.db
  (:require [reitit.core :as rt]
            [ook.reframe.router :as router]
            [ook.params.parse :as p]
            [clojure.set :as set]))

(def initial-db
  {:app/current-route (rt/map->Match {:template "/" :path "/" :data router/home-route-data})
   :results.datasets/data :loading})

(defn filters->query-params [db]
  (let [filters (:facets/applied db)]
    (when (seq filters)
      {:filters (p/serialize-filter-state filters)})))

(defn get-concept-tree [db facet-name codelist-uri]
  (-> db :facets/config (get facet-name) :codelists (get codelist-uri) :children))
