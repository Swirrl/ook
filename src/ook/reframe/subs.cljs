(ns ook.reframe.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  :ui.codes/query
  (fn [db _]
    (:ui.codes/query db)))

(rf/reg-sub
  :results.codes/data
  (fn [db _]
    (:results.codes/data db)))
