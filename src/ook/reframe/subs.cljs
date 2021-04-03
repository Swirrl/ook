(ns ook.reframe.subs
  (:require
   [re-frame.core :as rf]))

;;;;;; INITIAL, PERMANENT STATE

(rf/reg-sub :facets/config (fn [db _] (:facets/config db)))
(rf/reg-sub :datasets/count (fn [db _] (:datasets/count db)))

;;;;;; EPHEMERAL UI STATE

(rf/reg-sub
 :ui.facets/current
 (fn [db _]
   (:ui.facets/current db)))

(rf/reg-sub
  :ui.facets.current/codelist-selected?
  (fn [db [_ codelist]]
    (-> db :ui.facets/current :selection (get codelist) boolean)))

(rf/reg-sub
  :ui.facets.current/code-expanded?
  (fn [db [_ uri]]
    (-> db :ui.facets/current :expanded (get uri) boolean)))

(rf/reg-sub
  :ui.facets.current/tree
  (fn [db [_ facet-name]]
    (get-in db [:facets.codes/tree facet-name])))

;; (rf/reg-sub :ui.codes/query (fn [db _]
;;                               (:ui.codes/query db)))

;; (rf/reg-sub :ui.codes/selection (fn [db _]
;;                                   (:ui.codes/selection db)))

;; (rf/reg-sub :results.codes/data (fn [db _]
;;                                   (:results.codes/data db)))

;; (rf/reg-sub :results.codes/query (fn [db _]
;;                                    (:results.codes/query db)))

;;;;;; FACETS

(rf/reg-sub :facets/applied (fn [db _]
                              (:facets/applied db)))

;;;;;; DATASETS

(rf/reg-sub :results.datasets/data (fn [db _]
                                     (:results.datasets/data db)))

(rf/reg-sub :results.datasets/error (fn [db _]
                                      (:results.datasets/error db)))

;;;;;; NAVIGATION

(rf/reg-sub :app/current-route (fn [db _]
                                 (:app/current-route db)))
