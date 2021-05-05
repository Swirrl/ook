(ns ook.reframe.facets.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [ook.reframe.facets.db :as db]
   [ook.reframe.codes.db.caching :as caching]
   [ook.reframe.events :as e]))

;;; CLICK HANDLERS

(rf/reg-event-fx
 :ui.event/select-facet
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ next-facet-name]]
   (let [current-facet (:ui.facets/current db)
         codelists-cached? (caching/codelists-cached? db next-facet-name)
         facet-ui {:name next-facet-name}
         db-effect (if codelists-cached?
                     [:dispatch [:ui.facets/set-current facet-ui]]
                     [:dispatch-later {:ms 300 :dispatch [:ui.facets/set-current facet-ui]}])]
     {:fx [db-effect
           (when-not codelists-cached?
             [:dispatch [:ui.facets.current/get-codelists next-facet-name]])
           [:dispatch [:facets/apply-facet current-facet]]]})))

(rf/reg-event-fx
 :ui.event/edit-facet
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ facet-name]]
   (let [facet-ui (db/set-applied-selection-and-disclosure db facet-name)]
     {:db (assoc db :ui.facets/current facet-ui)
      :fx [(when-not (caching/codelists-cached? db facet-name)
             [:dispatch [:ui.facets.current/get-codelists facet-name]])
           [:dispatch [:ui.codes/get-all-code-trees-with-selections facet-ui]]]})))

(rf/reg-event-db
 :ui.event/cancel-current-selection
 [e/validation-interceptor]
 (fn [db _]
   (dissoc db :ui.facets/current)))

(rf/reg-event-db
 :ui.facets/set-current
 [e/validation-interceptor]
 (fn [db [_ facet-ui]]
   (assoc db :ui.facets/current facet-ui)))

;;; GETTING CODELISTS

(rf/reg-event-fx
 :ui.facets.current/get-codelists
 [e/validation-interceptor]
 (fn [_ [_ name]]
   {:fx [[:dispatch-later {:ms 300 :dispatch [:ui.facets.current/set-loading name]}]
         [:dispatch [:http/fetch-codelists name]]]}))

(rf/reg-event-db
 :ui.facets.current/set-loading
 [e/validation-interceptor]
 (fn [db [_ facet-name]]
   (let [status (get-in db [:ui.facets/status facet-name])]
     (if (#{:ready :error} status)
       db
       (assoc-in db [:ui.facets/status facet-name] :loading)))))

;;; APPLYING A FACET

(rf/reg-event-fx
 :facets/apply-facet
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ {:keys [name selection]}]]
   (when (seq selection)
     {:db (assoc-in db [:facets/applied name] selection)
      :dispatch [:app/navigate :ook.route/search]})))

;;; HTTP REQUESTS & RESPONSES

(rf/reg-event-fx
 :http/fetch-codelists
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ name]]
   (let [dimensions (db/get-dimensions db name)]
     {:http-xhrio {:method :get
                   :uri "/codelists"
                   :params {:dimension dimensions}
                   :response-format (ajax/transit-response-format)
                   :on-success [:http.codelists/success name]
                   :on-failure [:http.codelists/error name]}})))

;; HTTP RESPONSE HANDLERS

(rf/reg-event-db
 :http.codelists/success
 [e/validation-interceptor]
 (fn [db [_ name response]]
   (let [codelists (if (seq response) response :no-codelists)]
     (-> db
         (caching/cache-codelists name codelists)
         (assoc-in [:ui.facets/status name] :ready)))))

(rf/reg-event-db
 :http.codelists/error
 [e/validation-interceptor]
 (fn [db [_ name]]
   (assoc-in db [:ui.facets/status name] :error)))
