(ns ook.reframe.events
  (:require
   [re-frame.core :as rf]
   [ook.reframe.db :as db]
   [ajax.core :as ajax]
   [clojure.spec.alpha :as s]
   [ook.params.parse :as p]
   [reitit.frontend.easy :as rtfe]
   [day8.re-frame.http-fx]
   [ook.spec]))

;;;;;; VALIDATION

(defn- validate
  "Throws an exception (in development only) if `db` does not match the given spec"
  [spec db]
  (when ^boolean goog/DEBUG
    (when-let [error (s/explain-data spec db)]
      (throw (ex-info  (str "db spec validation failed: " (s/explain-str spec db))
                       error)))))

(def validation-interceptor (rf/after (partial validate :ook.spec/db)))

;;;;;; INITIALIZATION

(rf/reg-event-db
 :init/initialize-db
 [validation-interceptor]
 (fn [_ [_ {:keys [facets dataset-count]}]]
   (-> db/initial-db (assoc :facets/config facets
                            :datasets/count dataset-count))))

(rf/reg-event-fx
 :filters/reset
 [validation-interceptor]
 (fn [_ _]
   {:fx [[:dispatch [:filters/apply-filter-state {}]]
         [:dispatch [:app/navigate :ook.route/home]]]}))

;;;;;; UI MANAGEMENT

(rf/reg-event-fx
 :ui.datasets/remove-facet
 [validation-interceptor]
 (fn [{:keys [db]} [_ facet-name]]
   {:db (update db :facets/applied dissoc facet-name)
    :dispatch [:app/navigate :ook.route/search]}))

(rf/reg-event-fx
 :datasets/get-datasets
 [validation-interceptor]
 (fn [{:keys [db]} [_ filters]]
   {:db (assoc db :ui.datasets/loading true)
    :fx [[:dispatch-later {:ms 200 :dispatch [:ui.datasets/set-loading]}]
         [:dispatch [:http/fetch-datasets filters]]]}))

(rf/reg-event-db
 :ui.datasets/set-loading
 [validation-interceptor]
 (fn [db _]
   (if (:ui.datasets/loading db)
     (assoc db :results.datasets/data :loading)
     db)))

;;; HTTP REQUESTS/RESPONSES

(rf/reg-event-db
 :results.datasets.request/success
 [validation-interceptor]
 (fn [db [_ result]]
   (-> db
       (dissoc :ui.datasets/loading)
       (assoc :results.datasets/data result))))

(rf/reg-event-db
 :results.datasets.request/error
 [validation-interceptor]
 (fn [db [_ error]]
   (-> db
       (dissoc :ui.datasets/loading)
       (assoc :results.datasets/data :error))))

(rf/reg-event-fx
 :http/fetch-datasets
 [validation-interceptor]
 (fn [_ [_ filters]]
   {:http-xhrio {:method :get
                 :uri "/datasets"
                 :params (when (seq filters) {:filters filters})
                 :response-format (ajax/transit-response-format)
                 :on-success [:results.datasets.request/success]
                 :on-failure [:results.datasets.request/error]}}))

(rf/reg-event-fx
 :filters/apply-filter-state
 [validation-interceptor]
 (fn [{db :db} [_ filter-state]]
   {:db (-> db
            (assoc :facets/applied (p/deserialize-filter-state filter-state))
            (dissoc :ui.facets/current))
    :dispatch [:datasets/get-datasets filter-state]}))

;;;; NAVIGATION

(rf/reg-event-db
 :app/navigated
 [validation-interceptor]
 (fn [db [_ new-match]]
   (assoc db :app/current-route new-match)))

(rf/reg-event-fx
 :app/navigate
 [validation-interceptor]
 (fn [{:keys [db]} [_ route]]
   (let [query-params (db/filters->query-params db)]
     {:app/navigate! (cond-> {:route route}
                       (= :ook.route/search route) (merge {:query query-params}))})))

(rf/reg-fx
 :app/navigate!
 (fn [{:keys [route query]}]
   (rtfe/push-state route {} query)))
