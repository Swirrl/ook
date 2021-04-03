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

(defn all-expandable-uris [tree]
  (let [walk (fn walk* [node]
               (when-let [children (:children node)]
                 (cons (:ook/uri node)
                       (mapcat walk* children))))]
    (set (mapcat walk tree))))

(defn- all-uris [tree]
  (let [walk (fn walk* [node]
               (cons (:ook/uri node)
                     (when-let [children (:children node)]
                       (mapcat walk* children))))]
    (set (mapcat walk tree))))

(defn uri->child-uris [db uri]
  (let [tree (-> db :ui.facets/current :tree)
        walk (fn walk* [node]
               (when-let [children (:children node)]
                 (if (= (:ook/uri node) uri)
                   (all-uris children)
                   (mapcat walk* children))))]
    (set (mapcat walk tree))))

(defn uri->expandable-child-uris [db uri]
  (let [tree (-> db :ui.facets/current :tree)
        walk (fn walk* [node]
               (when-let [children (:children node)]
                 (if (= (:ook/uri node) uri)
                   (all-expandable-uris children)
                   (mapcat walk* children))))]
    (set (mapcat walk tree))))

(defn code-expanded? [db uri]
  (-> db :ui.facets/current :expanded (get uri) boolean))

(defn code-selected? [db uri]
  (-> db :ui.facets/current :selection (get uri) boolean))

(defn facet-by-name [db name]
   ;; would be easier if facet configs were indexed by name.. maybe change that?
  (->> db :facets/config (filter #(= (:name %) name)) first))
