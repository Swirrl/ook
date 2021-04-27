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

(defn all-expandable-uris [tree]
  (let [walk (fn walk* [node]
               (let [children (:children node)]
                 (when-not (keyword? children)
                   (cons (:ook/uri node)
                         (mapcat walk* children)))))]
    (set (mapcat walk tree))))

(defn all-uris [tree]
  (let [walk (fn walk* [node]
               (cons (:ook/uri node)
                     (let [children (:children node)]
                       (when-not (keyword? children)
                         (mapcat walk* children)))))]
    (set (mapcat walk tree))))

(defn- collect-children [uri collect-fn]
  (fn walk* [node]
    (let [children (:children node)]
      (when-not (keyword? children)
        (if (= (:ook/uri node) uri)
          (collect-fn children)
          (mapcat walk* children))))))

(defn uri->child-uris [db uri]
  (let [codelists (-> db :ui.facets/current :codelists vals)
        walk (collect-children uri all-uris)]
    (set (mapcat walk codelists))))

(defn uri->expandable-child-uris [db uri]
  (let [codelists (-> db :ui.facets/current :codelists vals)
        walk (collect-children uri all-expandable-uris)]
    (set (mapcat walk codelists))))

(defn code-expanded? [db uri]
  (-> db :ui.facets/current :expanded (get uri) boolean))

(defn collapse-children [db uri]
  (let [to-collapse (cons uri (uri->expandable-child-uris db uri))]
    (update-in db [:ui.facets/current :expanded] #(apply disj % to-collapse))))

(defn get-concept-tree [db facet-name codelist-uri]
  (-> db :facets/config (get facet-name) :codelists (get codelist-uri) :children))
