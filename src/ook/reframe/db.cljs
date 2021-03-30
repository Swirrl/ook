(ns ook.reframe.db
  (:require [ook.params.util :as pu]
            [reitit.core :as rt]
            [ook.reframe.router :as router]
            [clojure.spec.alpha :as s]))

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

;;;;; SPEC

(s/def :ook/db (s/keys :req [:facets/config
                             :app/current-route
                             (or :results.datasets/data :results.datasets/error)]

                       :opt [:facets/applied
                             :ui.facets/current]))

;;;;;;; facets

(s/def :facets/config (s/coll-of :ook/facet))

(s/def :ook/facet (s/keys :req-un [:facet/name :facet/dimensions :facet/codelists]))
(s/def :facet/name string?)
(s/def :facet/dimensions (s/coll-of string?))
(s/def :facet/codelists (s/coll-of :facet/codelist))
(s/def :facet/codelist (s/keys :req-un [:codelist/id :codelist/label]))
(s/def :codelist/id string?)
(s/def :codelist/label string?)

(s/def :facets/applied (s/nilable (s/map-of :facet/name :facet/selection)))

(s/def :ui.facets/current (s/nilable (s/and :ook/facet (s/keys :req-un [:facet/selection]))))

(s/def :facet/selection (s/coll-of :codelist/id))

;;;;;;; route

(s/def :app/current-route #(instance? reitit.core.Match %))

;;;;;;; datasets

(s/def :results.datasets/data (s/coll-of :ook/dataset))
(s/def :ook/dataset map?)

(s/def :results.datasets/error map?)
