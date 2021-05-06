(ns ook.reframe.events
  (:require
   [re-frame.core :as rf]
   [ook.reframe.db :as db]
   [clojure.spec.alpha :as s]
   [reitit.frontend.easy :as rtfe]
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
