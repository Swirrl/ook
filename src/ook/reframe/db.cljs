(ns ook.reframe.db
  (:require [ook.params.util :as pu]
            [reitit.core :as rt]
            [ook.reframe.router :as router]
            [ook.concerns.transit :as t]))

(def initial-db
  {:app/current-route (rt/map->Match {:template "/" :path "/" :data router/home-route-data})
   :results.datasets/data :loading})

(defn filters->query-params [db]
  (let [filters (:facets/applied db)]
    (when (seq filters)
      {:filters (t/write-string filters)})))

(defn all-expandable-uris [tree]
  (let [walk (fn walk* [node]
               (when-let [children (:children node)]
                 (cons (:ook/uri node)
                       (mapcat walk* children))))]
    (set (mapcat walk tree))))

(defn all-uris [tree]
  (let [walk (fn walk* [node]
               (cons (:ook/uri node)
                     (when-let [children (:children node)]
                       (mapcat walk* children))))]
    (set (mapcat walk tree))))

(defn uri->child-uris [db uri]
  (let [codelists (-> db :ui.facets/current :codelists vals)
        walk (fn walk* [node]
               (when-let [children (:children node)]
                 (if (= (:ook/uri node) uri)
                   (all-uris children)
                   (mapcat walk* children))))]
    (set (mapcat walk codelists))))

(defn uri->expandable-child-uris [db uri]
  (let [codelists (-> db :ui.facets/current :codelists vals)
        walk (fn walk* [node]
               (when-let [children (:children node)]
                 (if (= (:ook/uri node) uri)
                   (all-expandable-uris children)
                   (mapcat walk* children))))]
    (set (mapcat walk codelists))))

(defn code-expanded? [db uri]
  (-> db :ui.facets/current :expanded (get uri) boolean))

(defn facet-by-name [db name]
   ;; would be easier if facet configs were indexed by name.. maybe change that?
  (->> db :facets/config (filter #(= (:name %) name)) first))

(defn collapse-children [db uri]
  (let [to-collapse (cons uri (uri->expandable-child-uris db uri))]
    (update-in db [:ui.facets/current :expanded] #(apply disj % to-collapse))))
