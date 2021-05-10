(ns ook.reframe.subs
  (:require
   [re-frame.core :as rf]
   [clojure.set :as set]
   [ook.reframe.facets.db :as facets]
   [ook.reframe.codes.db.selection :as selection]
   [ook.reframe.codes.db.disclosure :as disclosure]
   [ook.reframe.codes.search.db :as search]))

;;;;;; INITIAL, PERMANENT STATE

(rf/reg-sub
 :facets/config
 (fn [db _]
   (->> db :facets/config vals (sort-by :sort-priority))))

(rf/reg-sub
 :datasets/count
 (fn [db _]
   (:datasets/count db)))

;;;;;; EPHEMERAL UI STATE - FACETS

(rf/reg-sub
 :ui.facets.current/status
 (fn [db _]
   (let [facet-name (-> db :ui.facets/current :name)]
     (-> db :ui.facets/status (get facet-name)))))

(rf/reg-sub
 :ui.facets.current/name
 (fn [db _]
   (some-> db :ui.facets/current :name)))

(rf/reg-sub
 :ui.facets.current/selection
 (fn [db _]
   (some-> db :ui.facets/current :selection)))

(rf/reg-sub
 :ui.facets.current/option-selected?
 (fn [db [_ option]]
   (selection/option-selected? (:ui.facets/current db) option)))

(rf/reg-sub
 :ui.facets.current/option-expanded?
 (fn [db [_ uri]]
   (disclosure/expanded? (:ui.facets/current db) uri)))

(rf/reg-sub
 :ui.facets.current/all-used-children-selected?
 (fn [db [_ {:keys [scheme children] :as code}]]
   (let [child-uris (->> children (map :ook/uri) set)
         current-selection (-> db :ui.facets/current :selection (get scheme))]
     (set/subset? child-uris current-selection))))

(rf/reg-sub
 :ui.facets.current/any-used-children?
 (fn [db [_ code]]
   (-> code selection/used-child-uris seq boolean)))

;;;;;; CODES

(rf/reg-sub
 :ui.codes/status
 (fn [db [_ codelist-uri]]
   (-> db :ui.codes/status (get codelist-uri))))

;;;;;; SEARCH

(rf/reg-sub
  :ui.facets.current/search-term
  (fn [db _]
    (some-> db :ui.facets/current :codes/search :search-term)))

(rf/reg-sub
 :ui.facets.current/search-status
 (fn [db _]
   (some-> db :ui.facets/current :codes/search :status)))

(rf/reg-sub
 :ui.facets.current/search-results
 (fn [db _]
   (search/get-results db)))

(rf/reg-sub
 :ui.facets.current/search-result?
 (fn [db [_ uri]]
   (let [result-uris (->> db search/get-results (map :ook/uri) set)]
     (boolean (get result-uris uri)))))

;;;;;; FACETS

(rf/reg-sub
 :facets/applied
 (fn [db _]
   (:facets/applied db)))

(rf/reg-sub
 :ui.facets.current/visible-codes
 (fn [db [_ name]]
   (let [all-codelists (facets/get-codelists db name)
         search-results (search/get-results db)]
     (cond
       (= :no-codelists all-codelists) all-codelists

       (seq search-results) (search/filter-to-search-results all-codelists search-results)

       :else (sort-by :ook/uri all-codelists)))))

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
