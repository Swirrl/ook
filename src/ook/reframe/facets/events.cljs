(ns ook.reframe.facets.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [ook.reframe.db :as db]
   [ook.reframe.codes.db.caching :as caching]
   [ook.reframe.events :as e]))

;;; CLICK HANDLERS

(rf/reg-event-fx
 :ui.event/set-current
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ {:keys [codelists] :as next-facet}]]
   (let [{:keys [selection] :as current-facet} (:ui.facets/current db)]
     (if codelists
       (cond-> {:db (db/set-current-facet db next-facet)}
         (seq selection) (merge {:dispatch [:facets/apply-facet current-facet]}))
       {:fx [[:dispatch [:ui.facets.current/get-codelists next-facet]]
             (when (seq selection)
               [:dispatch [:facets/apply-facet current-facet]])]}))))

(rf/reg-event-db
 :ui.event/cancel-current-selection
 [e/validation-interceptor]
 (fn [db _]
   (dissoc db :ui.facets/current :ui.facets.current/status)))

;;; GETTING CODELISTS

(rf/reg-event-fx
 :ui.facets.current/get-codelists
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ facet]]
   {:db (assoc db :facets.current/loading true)
    :fx [[:dispatch-later {:ms 300 :dispatch [:ui.facets.current/set-loading]}]
         [:dispatch [:http/fetch-codelists facet]]]}))

(rf/reg-event-db
 :ui.facets.current/set-loading
 [e/validation-interceptor]
 (fn [db _]
   (if (:facets.current/loading db)
     (assoc db :ui.facets.current/status :loading)
     db)))

;;; APPLYING A FACET

(rf/reg-event-fx
 :facets/apply-facet
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ {:keys [name selection]}]]
   {:db (cond-> db
          (seq selection) (assoc-in [:facets/applied name] selection))
    :dispatch [:app/navigate :ook.route/search]}))

;;; HTTP REQUESTS & RESPONSES

(rf/reg-event-fx
 :http/fetch-codelists
 [e/validation-interceptor]
 (fn [_ [_ {:keys [name dimensions]}]]
   {:http-xhrio {:method :get
                 :uri "/codelists"
                 :params {:dimension dimensions}
                 :response-format (ajax/transit-response-format)
                 :on-success [:http.codelists/success name]
                 :on-failure [:http.codelists/error]}}))

;; HTTP RESPONSE HANDLERS

(rf/reg-event-db
 :http.codelists/success
 [e/validation-interceptor]
 (fn [db [_ facet-name response]]
   (let [status (if (empty? response) :success/empty :success/ready)
         updated-db (caching/cache-codelist db facet-name response)]
     (-> updated-db
         (dissoc :facets.current/loading)
         (assoc :ui.facets.current/status status)
         (assoc :ui.facets/current (-> updated-db :facets/config (get facet-name)))))))

(rf/reg-event-db
 :http.codelists/error
 [e/validation-interceptor]
 (fn [db [_ error]]
   (assoc db :ui.facets.current/status :error)))
