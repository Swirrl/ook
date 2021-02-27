(ns ook.reframe.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  :app/current-route
  (fn [db _]
    (:app/current-route db)))

(rf/reg-sub
  :ui.codes/query
  (fn [db _]
    (:ui.codes/query db)))

(rf/reg-sub
  :results.codes/data
  (fn [db _]
    (:results.codes/data db)))
