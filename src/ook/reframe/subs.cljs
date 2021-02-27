(ns ook.reframe.subs
  (:require
   [re-frame.core :as rf]
   [ook.reframe.db :as db]))

(rf/reg-sub :app/current-route (fn [db _]
                                 (:app/current-route db)))

(rf/reg-sub :ui.codes/query (fn [db _]
                              (:ui.codes/query db)))

(rf/reg-sub :ui.codes/selection (fn [db _]
                                  (:ui.codes/selection db)))

(rf/reg-sub :results.codes/data (fn [db _]
                                  (:results.codes/data db)))

(rf/reg-sub :results.codes/query (fn [db _]
                                   (:results.codes/query db)))

(rf/reg-sub :results.datasets/data (fn [db _]
                                     (:results.datasets/data db)))
