(ns ook.reframe.subs
  (:require
   [re-frame.core :as rf]
   [ook.reframe.db :as db]))

;;;;;; INITIAL, PERMANENT STATE

(rf/reg-sub
 :facets/config
 (fn [db _]
   (->> db :facets/config (sort-by :name))))

(rf/reg-sub
 :datasets/count
 (fn [db _]
   (:datasets/count db)))

;;;;;; EPHEMERAL UI STATE

(rf/reg-sub
 :ui.facets/current
 (fn [db _]
   (if-let [facet (:ui.facets/current db)]
     (update facet :tree #(sort-by :ook/uri %))
     db)))

(rf/reg-sub
 :ui.facets.current/codelist-selected?
 (fn [db [_ uri]]
   (db/code-selected? db uri)))

(rf/reg-sub
 :ui.facets.current/code-expanded?
 (fn [db [_ uri]]
   (db/code-expanded? db uri)))

;;;;;; FACETS

(rf/reg-sub
 :facets/applied
 (fn [db _]
   (:facets/applied db)))

;;;;;; DATASETS

(rf/reg-sub
 :results.datasets/data
 (fn [db _]
   (:results.datasets/data db)))

(rf/reg-sub
 :results.datasets/error
 (fn [db _]
   (:results.datasets/error db)))

;;;;;; NAVIGATION

(rf/reg-sub
 :app/current-route
 (fn [db _]
   (:app/current-route db)))
