(ns ook.reframe.db
  (:require [reitit.core :as rt]
            [ook.reframe.router :as router]
            [ook.params.parse :as p]))

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

(defn uri->child-uris [db uri]
  (let [codelists (-> db :ui.facets/current :codelists vals)
        walk (fn walk* [node]
               (let [children (:children node)]
                 (when-not (keyword? children)
                   (if (= (:ook/uri node) uri)
                     (all-uris children)
                     (mapcat walk* children)))))]
    (set (mapcat walk codelists))))

(defn uri->expandable-child-uris [db uri]
  (let [codelists (-> db :ui.facets/current :codelists vals)
        walk (fn walk* [node]
               (let [children (:children node)]
                 (when-not (keyword? children)
                   (if (= (:ook/uri node) uri)
                     (all-expandable-uris children)
                     (mapcat walk* children)))))]
    (set (mapcat walk codelists))))

;; (defn expanded-uris [facet selection]
;;   (let [codelists (-> facet :codelists vals)
;;         open-uris (-> selection vals flatten)]
;;     )
;;   )

(defn code-expanded? [db uri]
  (-> db :ui.facets/current :expanded (get uri) boolean))

(defn collapse-children [db uri]
  (let [to-collapse (cons uri (uri->expandable-child-uris db uri))]
    (update-in db [:ui.facets/current :expanded] #(apply disj % to-collapse))))

(defn set-current-facet [db facet]
  (let [status (if (empty? (:codelists facet)) :success/empty :success/ready)]
    (-> db
        (assoc :ui.facets/current facet)
        (assoc :ui.facets.current/status status))))
