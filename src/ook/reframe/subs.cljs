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
 :ui.facets.current/checked-state
 (fn [db [_ option]]
   (let [selected-uris (selection/all-selected-uris db)]
     (cond
       (selection/option-selected? (:ui.facets/current db) option)
       :checked

       (selection/indeterminate? selected-uris option)
       :indeterminate

       :else
       :unchecked))))

(rf/reg-sub
 :ui.facets.current/option-expanded?
 (fn [db [_ uri]]
   (disclosure/expanded? (:ui.facets/current db) uri)))

(rf/reg-sub
 :ui.facets.current/all-used-children-selected?
 (fn [db [_ {:keys [scheme children]}]]
   (let [used-child-uris (->> children (filter :used) (map :ook/uri) set)
         current-selection (-> db :ui.facets/current :selection (get scheme))]
     (set/subset? used-child-uris current-selection))))

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
 :ui.facets.current/any-results?
 (fn [db _]
   (boolean (seq (search/get-results db)))))

(rf/reg-sub
 :ui.facets.current/search-result?
 (fn [db [_ uri]]
   (let [result-uris (->> db search/get-results (map :ook/uri) set)]
     (boolean (get result-uris uri)))))

(rf/reg-sub
 :ui.facets.current/search-result-code-count
 (fn [db _]
   (->> db search/get-results count)))

(rf/reg-sub
 :ui.facets.current/search-result-codelist-count
 (fn [db _]
   (->> db search/get-results (map :scheme) set count)))

(rf/reg-sub
 :ui.facets.current/filtered-codelists
 (fn [db [_ facet-name]]
   (let [all-codelists (facets/get-codelists db facet-name)
         search-results (search/get-results db)]
     (search/filter-to-search-results all-codelists search-results))))

(rf/reg-sub
 :ui.search/all-matches-selected?
 (fn [db _]
   (let [used-search-result-uris (->> db search/get-results (filter :used) (map :ook/uri) set)
         current-selection (->> db :ui.facets/current :selection vals (reduce concat) set)]
     (set/subset? used-search-result-uris current-selection))))

(rf/reg-sub
 :ui.search/any-matches-selected?
 (fn [db _]
   (let [used-search-result-uris (->> db search/get-results (filter :used) (map :ook/uri) set)
         current-selection (->> db :ui.facets/current :selection vals (reduce concat) set)]
     (->> current-selection (filter used-search-result-uris) seq boolean))))

;;;;;; FACETS

(rf/reg-sub
 :facets/applied
 (fn [db _]
   (:facets/applied db)))

(rf/reg-sub
 :ui.facets/no-codelists?
 (fn [db [_ facet-name]]
   (= :no-codelists (facets/get-codelists db facet-name))))

(rf/reg-sub
 :ui.facets/codelists
 (fn [db [_ facet-name]]
   (->> (facets/get-codelists db facet-name) (sort-by :label))))

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
