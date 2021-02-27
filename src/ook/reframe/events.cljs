(ns ook.reframe.events
  (:require [re-frame.core :as rf]
            [ook.reframe.db :as db]))

(rf/reg-event-db
  :init/initialize-db
  (fn [_ _] db/initial-state))

;;;;;

(rf/reg-event-db
  :ui.codes/query-change
  (fn [db [_ new-query]]
    (assoc db :ui.codes/query new-query)))

(rf/reg-event-db
  :codes/submit-search
  (fn [_ _]
    ,,,))
