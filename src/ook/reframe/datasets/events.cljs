(ns ook.reframe.datasets.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [ook.params.parse :as p]
   [ook.reframe.events :as e]
   [ook.spec]))

;;; CLICK HANDLERS

(rf/reg-event-fx
 :ui.event/clear-filters
 [e/validation-interceptor]
 (fn [_ _]
   {:fx [[:dispatch [:filters/apply-filter-state {}]]
         [:dispatch [:app/navigate :ook.route/home]]]}))

(rf/reg-event-fx
 :ui.event/remove-facet
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ facet-name]]
   {:db (update db :facets/applied dissoc facet-name)
    :dispatch [:app/navigate :ook.route/search]}))

;;; FILTERING DATASETS

(rf/reg-event-fx
 :filters/apply-filter-state
 [e/validation-interceptor]
 (fn [{db :db} [_ filter-state]]
   {:db (assoc db :facets/applied (p/deserialize-filter-state filter-state))
    :dispatch [:datasets/get-datasets filter-state]}))

(rf/reg-event-fx
 :datasets/get-datasets
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ filters]]
   {:db (assoc db :ui.datasets/loading true)
    :fx [[:dispatch-later {:ms 200 :dispatch [:ui.datasets/set-loading]}]
         [:dispatch [:http/fetch-datasets filters]]]}))

(rf/reg-event-db
 :ui.datasets/set-loading
 [e/validation-interceptor]
 (fn [db _]
   (if (:ui.datasets/loading db)
     (assoc db :results.datasets/data :loading)
     db)))


(rf/reg-event-fx
 :http/fetch-datasets
 [e/validation-interceptor]
 (fn [_ [_ filters]]
   {:http-xhrio {:method :get
                 :uri "/datasets"
                 :params (when (seq filters) {:filters filters})
                 :response-format (ajax/transit-response-format)
                 :on-success [:http.datsets/success]
                 :on-failure [:http.datasets/error]}}))

(rf/reg-event-db
 :http.datasets/success
 [e/validation-interceptor]
 (fn [db [_ result]]
   (-> db
       (dissoc :ui.datasets/loading)
       (assoc :results.datasets/data result))))

(rf/reg-event-db
 :http.datasets/error
 [e/validation-interceptor]
 (fn [db [_ error]]
   (-> db
       (dissoc :ui.datasets/loading)
       (assoc :results.datasets/data :error))))
