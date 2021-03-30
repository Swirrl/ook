(ns ook.reframe.events
  (:require [re-frame.core :as rf]
            [ook.reframe.db :as db]
            [ajax.core :as ajax]
            [ook.util :as u]
            [clojure.spec.alpha :as s]
            [ook.params.parse :as p]
            [reitit.frontend.easy :as rtfe]
            [day8.re-frame.http-fx]))

;;;;;; VALIDATION

(defn- validate
  "Throws an exception (in development only) if `db` does not match the given spec"
  [spec db]
  (when ^boolean goog/DEBUG
    (when-let [error (s/explain-data spec db)]
      (throw (ex-info  (str "db spec validation failed: " (s/explain-str spec db))
                       error)))))

(def validation-interceptor (rf/after (partial validate :ook/db)))

;;;;;; INITIALIZATION

(rf/reg-event-db
 :init/initialize-db
 [validation-interceptor]
 (fn [_ [_ {:keys [facets dataset-count]}]]
   (-> db/initial-db (assoc :facets/config facets
                            :datasets/count dataset-count))))

;;;;;; FACETS

(rf/reg-event-db
 :ui.facets/set-current
 [validation-interceptor]
 (fn [db [_ {:keys [codelists] :as facet}]]
   ;; (let [next-id (->> db :facets (map :id) (cons 0) (apply max) inc)])
   (if facet
     (let [with-selection (assoc facet :selection (->> codelists
                                                       (map :id)
                                                       set))]
       (assoc db :ui.facets/current with-selection))
     (dissoc db :ui.facets/current))))

(rf/reg-event-db
 :ui.facets.current/toggle-selection
 [validation-interceptor]
 (fn [db [_ val]]
   (let [selected? (-> db :ui.facets/current :selection (get val))
         update-fn (if selected? disj conj)]
     (update-in db [:ui.facets/current :selection] update-fn val))))

(rf/reg-event-db
 :filters/add-current-facet
 [validation-interceptor]
 (fn [db _]
   (let [current-facet (:ui.facets/current db)]
     (if-let [selection (seq (:selection current-facet))]
       (assoc-in db [:facets/applied (:name current-facet)] selection)
       db))))

(rf/reg-event-db
 :filters/remove-facet
 [validation-interceptor]
 (fn [db [_ facet-name]]
   (update db :facets/applied dissoc facet-name)))

;;;;; UI STATE MANAGEMENT

;; (rf/reg-event-db :ui.codes/query-change (fn [db [_ new-query]]
;;                                           (assoc db :ui.codes/query new-query)))

;; (rf/reg-event-db :ui.codes/toggle-selection (fn [db [_ val]]
;;                                               (update-in db [:ui.codes/selection val] not)))

;; (rf/reg-event-db :ui.codes/set-selection (fn [db [_ selection]]
;;                                            (assoc db :ui.codes/selection selection)))

;; (rf/reg-event-db :ui.codes.selection/reset (fn [db _]
;;                                              (dissoc db :ui.codes/selection)))

;; (rf/reg-event-db :results.datasets/reset (fn [db _]
;;                                            (dissoc db :results.datasets/data :ui.codes/selection)))

;;; HTTP REQUESTS/RESPONSES

;; (rf/reg-event-db :results.codes.request/success (fn [db [_ query result]]
;;                                                   (assoc db
;;                                                          :results.codes/data result
;;                                                          :results.codes/query query)))

;; (rf/reg-event-db :results.codes.request/error (fn [db [_ error]]
;;                                                 (assoc db :results.codes/error error)))

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

;;;;; HTTP REQUESTS

;; (rf/reg-event-fx :codes/submit-search (fn [_ [_ query]]
;;                                         {:http-xhrio {:method :get
;;                                                       :uri "/get-codes"
;;                                                       :params {:q query}
;;                                                       :response-format (ajax/transit-response-format)
;;                                                       :on-success [:results.codes.request/success query]
;;                                                       :on-failure [:results.codes.request/errror]}
;;                                          :fx [[:dispatch [:ui.codes/query-change query]]
;;                                               [:dispatch [:results.datasets/reset]]]}))

;; (rf/reg-event-fx :filters/apply-code-selection (fn [_ [_ facets]]
;;                                                  {:http-xhrio {:method :get
;;                                                                :uri "/apply-filters"
;;                                                                :params {:facet facets}
;;                                                                :response-format (ajax/transit-response-format)
;;                                                                :on-success [:results.datasets.request/success]
;;                                                                :on-failure [:results.datasets.request/errror]}
;;                                                   :dispatch [:ui.codes/set-selection (zipmap (u/box facets) (repeat true))]}))

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
