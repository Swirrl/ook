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
   {:fx [[:dispatch [:filters/apply []]]
         [:dispatch [:app/navigate :ook.route/home]]]}))

;;;;;; FILTERS

(rf/reg-event-db
 :filters/remove-facet
 [validation-interceptor]
 (fn [db [_ facet-name]]
   (update db :facets/applied dissoc facet-name)))

;;; HTTP REQUESTS/RESPONSES

(rf/reg-event-db
 :results.datasets.request/success
 [validation-interceptor]
 (fn [db [_ result]]
   (-> db
       (dissoc :results.codes/error)
       (assoc :results.datasets/data result))))

(rf/reg-event-db
 :results.datasets.request/error
 [validation-interceptor]
 (fn [db [_ result]]
   (assoc db :results.datasets/error result)))

(rf/reg-event-fx
 :datasets/fetch-datasets
 [validation-interceptor]
 (fn [_ [_ facets]]
   {:http-xhrio {:method :get
                 :uri "/datasets"
                 :params (when facets {:facet facets})
                 :response-format (ajax/transit-response-format)
                 :on-success [:results.datasets.request/success]
                 :on-failure [:results.datasets.request/error]}}))

(rf/reg-event-fx
 :filters/apply
 [validation-interceptor]
 (fn [{db :db} [_ facets]]
   {:dispatch [:datasets/fetch-datasets facets]
    :db (-> db
            (assoc :facets/applied (p/parse-named-facets facets))
            (dissoc :ui.facets/current))}))

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
