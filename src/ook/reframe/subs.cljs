(ns ook.reframe.subs
  (:require
   [re-frame.core :as rf]
   [clojure.set :as set]
   [ook.reframe.db :as db]
   [ook.reframe.codes.db.selection :as selection]
   [ook.reframe.codes.db.disclosure :as disclosure]))

;;;;;; INITIAL, PERMANENT STATE

(rf/reg-sub
 :facets/config
 (fn [db _]
   (->> db :facets/config vals (sort-by :sort-priority))))

(rf/reg-sub
 :datasets/count
 (fn [db _]
   (:datasets/count db)))

;;;;;; EPHEMERAL UI STATE

(rf/reg-sub
 :ui.facets.current/status
 (fn [db _]
   (:ui.facets.current/status db)))

(rf/reg-sub
 :ui.facets.current/name
 (fn [db _]
   (some-> db :ui.facets/current :name)))

(rf/reg-sub
 :ui.facets.current/selection
 (fn [db _]
   (some-> db :ui.facets/current :selection)))

(rf/reg-sub
 :ui.facets.current/codelists
 (fn [db _]
   (some->> db :ui.facets/current :codelists vals (sort-by :ook/uri))))

(rf/reg-sub
 :ui.facets.current/option-selected?
 (fn [db [_ option]]
   (selection/option-selected? (:ui.facets/current db) option)))

(rf/reg-sub
 :ui.facets.current/option-expanded?
 (fn [db [_ uri]]
   (disclosure/expanded? (:ui.facets/current db) uri)))

(rf/reg-sub
 :ui.facets.current/all-children-selected?
 (fn [db [_ {:keys [scheme children]}]]
   (let [child-uris (->> children (map :ook/uri) set)
         current-selection (-> db :ui.facets/current :selection (get scheme))]
     (set/subset? child-uris current-selection))))

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

;;;;;; NAVIGATION

(rf/reg-sub
 :app/current-route
 (fn [db _]
   (:app/current-route db)))
