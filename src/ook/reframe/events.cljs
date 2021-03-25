(ns ook.reframe.events
  (:require [re-frame.core :as rf]
            [ook.reframe.db :as db]
            [ajax.core :as ajax]
            [ook.util :as u]
            [reitit.frontend.easy :as rtfe]
            [day8.re-frame.http-fx]))

;;;;;; INITIALIZATION

(rf/reg-event-db :init/set-facets (fn [db [_ facets]]
                                    (assoc db :facets/config facets)))

(rf/reg-event-fx :init/initialize-db (fn [_ [_ facets]]
                                       {:http-xhrio {:method :get
                                                     :uri "/datasets"
                                                     :response-format (ajax/transit-response-format)
                                                     :on-success [:results.datasets.request/success]
                                                     :on-error [:results.datasets.request/error]}
                                        :dispatch [:init/set-facets facets]}))

;;;;;; FACETS

(rf/reg-event-db
 :ui.facets/set-current
 (fn [db [_ {:keys [codelists] :as facet}]]
   ;; (let [next-id (->> db :facets (map :id) (cons 0) (apply max) inc)])
   (if facet
     (let [with-selection (assoc facet :selection (->> codelists (map :codelist) set))]
       (assoc db :ui.facets/current with-selection))
     (dissoc db :ui.facets/current))))

(rf/reg-event-db
 :ui.facets.current/toggle-selection
 (fn [db [_ val]]
   (let [selected? (-> db :ui.facets/current :selection (get val))
         update-fn (if selected? disj conj)]
     (update-in db [:ui.facets/current :selection] update-fn val))))

(rf/reg-event-db
  :filters/add-current-facet
  (fn [db _]
    (let [current-facet (:ui.facets/current db)]
      (assoc-in db
                [:facets/applied (:name current-facet)]
                (:selection current-facet)))))

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

(rf/reg-event-db :results.datasets.request/success (fn [db [_ result]]
                                                     (-> db
                                                         (dissoc :results.codes/error)
                                                         (assoc :results.datasets/data result))))

(rf/reg-event-db :results.datasets.request/error (fn [db [_ result]]
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
 :filters/apply
 (fn [{db :db} _]
   {:http-xhrio {:method :get
                 :uri "/apply-filters"
                 :params (db/filters->query-params db)
                 :response-format (ajax/transit-response-format)
                 :on-success [:results.datasets.request/success]
                 :on-failure [:results.datasets.request/error]}
    :db (dissoc db :ui.facets/current)}))

;;;; NAVIGATION

(rf/reg-event-db :app/navigated (fn [db [_ new-match]]
                                  (assoc db :app/current-route new-match)))

(rf/reg-event-fx :app/navigate (fn [{:keys [db]} [_ route]]
                                 (let [query-params (db/filters->query-params db)]
                                   {:app/navigate! (cond-> {:route route}
                                                     (= :ook.route/search route) (merge {:query query-params}))})))

(rf/reg-fx :app/navigate! (fn [{:keys [route query]}]
                            (rtfe/push-state route {} query)))
