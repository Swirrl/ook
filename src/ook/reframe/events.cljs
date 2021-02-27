(ns ook.reframe.events
  (:require [re-frame.core :as rf]
            [ook.reframe.db :as db]
            [ajax.core :as ajax]
            [reitit.frontend.easy :as rtfe]
            [day8.re-frame.http-fx]))

(rf/reg-event-db
  :init/initialize-db
  (fn [_ _] db/initial-state))

;;;;; UI STATE MANAGEMENT

(rf/reg-event-db
  :ui.codes/query-change
  (fn [db [_ new-query]]
    (assoc db :ui.codes/query new-query)))

(rf/reg-event-db
  :ui.codes/selection-change
  (fn [db [_ val]]
    (update-in db [:ui.codes/selection val] not)))

;;; HTTP RESPONSES

(rf/reg-event-db
 :results.codes.request/success
 (fn [db [_ query result]]
   (assoc db
          :results.codes/data result
          :results.codes/query query)))

(rf/reg-event-db
  :results.codes.request/error
  (fn [db [_ error]]
    (assoc-in db :results.codes/error error)))

(rf/reg-event-db
 :results.datasets.request/success
 (fn [db [_ result]]
   (assoc-in db :results.datasets/data result)))


(rf/reg-event-db
 :results.datasets.request/error
 (fn [db [_ result]]
   (assoc-in db :results.datasets/error result)))

;;;;; EFFECTS

(rf/reg-event-fx
 :codes/submit-search
 (fn [{:keys [db]} [_ query]]
   {:http-xhrio {:method :get
                 :uri "/search"
                 :params {:q query}
                 :response-format (ajax/transit-response-format)
                 :on-success [:results.codes.request/success query]
                 :on-error [:results.codes.request/errror]}
    :dispatch [:ui.codes/query-change query]}))

(rf/reg-event-fx
  :filters/apply-code-selection
 (fn [{:keys [db]} [_]]
   (let [codes (:ui.codes/selection db)]
     {:http-xhrio {:method :get
                   :uri "/apply-filters"
                   :params {:code codes}
                   :response-format (ajax/transit-response-format)
                   :on-success [:results.datasets.request/success]
                   :on-error [:results.datasets.request/errror]}})))

;;;; NAVIGATION

(rf/reg-event-db
 :app/navigated
 (fn [db [_ new-match]]
   (assoc db :app/current-route new-match)))

(rf/reg-event-fx
 :app/navigate
 (fn [_ [_ route query]]
   {:app/navigate! {:route route
                    :query query}}))

(rf/reg-fx
 :app/navigate!
 (fn [{:keys [route query]}]
   (rtfe/push-state route {} query)))
