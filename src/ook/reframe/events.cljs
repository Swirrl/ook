(ns ook.reframe.events
  (:require [re-frame.core :as rf]
            [ook.reframe.db :as db]
            [ajax.core :as ajax]
            [ook.util :as u]
            [reitit.frontend.easy :as rtfe]
            [day8.re-frame.http-fx]))

(rf/reg-event-db
 :init/initialize-db
 (fn [_ _] db/initial-state))

;;;;; UI STATE MANAGEMENT

(rf/reg-event-db :ui.codes/query-change (fn [db [_ new-query]]
                                          (assoc db :ui.codes/query new-query)))

(rf/reg-event-db :ui.codes/toggle-selection (fn [db [_ val]]
                                              (update-in db [:ui.codes/selection val] not)))

(rf/reg-event-db :ui.codes/set-selection (fn [db [_ selection]]
                                           (assoc db :ui.codes/selection selection)))

(rf/reg-event-db :ui.codes.selection/reset (fn [db _]
                                             (dissoc db :ui.codes/selection)))

(rf/reg-event-db :results.datasets/reset (fn [db _]
                                           (dissoc db :results.datasets/data :ui.codes/selection)))

;;; HTTP RESPONSES

(rf/reg-event-db :results.codes.request/success (fn [db [_ query result]]
                                                  (assoc db
                                                         :results.codes/data result
                                                         :results.codes/query query)))

(rf/reg-event-db :results.codes.request/error (fn [db [_ error]]
                                                (assoc db :results.codes/error error)))

(rf/reg-event-db :results.datasets.request/success (fn [db [_ result]]
                                                     (assoc db :results.datasets/data result)))

(rf/reg-event-db :results.datasets.request/error (fn [db [_ result]]
                                                   (assoc db :results.datasets/error result)))

;;;;; HTTP REQUESTS

(rf/reg-event-fx :codes/submit-search (fn [_ [_ query]]
                                        {:http-xhrio {:method :get
                                                      :uri "/get-codes"
                                                      :params {:q query}
                                                      :response-format (ajax/transit-response-format)
                                                      :on-success [:results.codes.request/success query]
                                                      :on-error [:results.codes.request/errror]}
                                         :fx [[:dispatch [:ui.codes/query-change query]]
                                              [:dispatch [:results.datasets/reset]]]}))

(rf/reg-event-fx :filters/apply-code-selection (fn [{:keys [db]} [_ codes]]
                                                 {:http-xhrio {:method :get
                                                               :uri "/apply-filters"
                                                               :params {:code codes}
                                                               :response-format (ajax/transit-response-format)
                                                               :on-success [:results.datasets.request/success]
                                                               :on-error [:results.datasets.request/errror]}
                                                  :dispatch [:ui.codes/set-selection (zipmap (u/box codes) (repeat true))]}))

;;;; NAVIGATION

(rf/reg-event-db :app/navigated (fn [db [_ new-match]]
                                  (assoc db :app/current-route new-match)))

(rf/reg-event-fx :app/navigate (fn [{:keys [db]} [_ route]]
                                 (let [query-params (db/->query-params db)]
                                   {:app/navigate! (cond-> {:route route}
                                                     (= :ook.route/search route) (merge {:query query-params}))})))

(rf/reg-fx :app/navigate! (fn [{:keys [route query]}]
                            (rtfe/push-state route {} query)))