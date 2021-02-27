(ns ook.reframe.events
  (:require [re-frame.core :as rf]
            [ook.reframe.db :as db]
            [ajax.core :as ajax]
            [reitit.frontend.easy :as rtfe]
            [day8.re-frame.http-fx]))

(rf/reg-event-db
  :init/initialize-db
  (fn [_ _] db/initial-state))

;;;;; SEARCHING

(rf/reg-event-db
  :ui.codes/query-change
  (fn [db [_ new-query]]
    (assoc db :ui.codes/query new-query)))



(rf/reg-event-db
  :app/navigated
  (fn [db [_ new-match]]
    (assoc db :app/current-route new-match)))

;;;;; EFFECTS

(rf/reg-event-fx
 :codes/submit-search
 (fn [{:keys [db]} [_]]
   (let [query (:ui.codes/query db)]
     {:http-xhrio {:method :get
                   :uri "/search"
                   :response-format (ajax/transit-response-format)
                   :on-success [:results.codes.request/success]
                   :on-error [:results.codes.request/errror]}})
   ))

(rf/reg-event-fx
  :app/navigate
  (fn [_ [_ route]]
    {:app/navigate! route}))

(rf/reg-fx
  :app/navigate!
  (fn [k params query]
    (rtfe/push-state k params query)))
