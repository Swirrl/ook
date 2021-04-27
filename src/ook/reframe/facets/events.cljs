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
 (fn [{:keys [db]} [_ {:keys [codelists] :as next-facet}]]
   (let [current-facet (:ui.facets/current db)]
     {:db (db/set-current-facet db next-facet)
      :fx [(when-not codelists
             [:dispatch [:ui.facets.current/get-codelists next-facet]])
           [:dispatch [:facets/apply-facet current-facet]]]})))

(rf/reg-event-fx
 :ui.event/edit-facet
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ {:keys [name] :as facet}]]
   (let [with-ui-state (db/set-applied-selection-and-disclosure db facet)]
     {:db (db/set-current-facet db with-ui-state)
      :fx [(when-not (caching/codelists-cached? db name)
             [:dispatch [:ui.facets.current/get-codelists with-ui-state]])
           (when-not (caching/selected-trees-cached? db with-ui-state)
             [:dispatch [:codes/get-concept-trees-with-selected-codes with-ui-state]])]})))

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
   (when (seq selection)
     {:db (assoc-in db [:facets/applied name] selection)
      :dispatch [:app/navigate :ook.route/search]})))

;;; HTTP REQUESTS & RESPONSES

(rf/reg-event-fx
 :http/fetch-codelists
 [e/validation-interceptor]
 (fn [_ [_ {:keys [dimensions] :as facet}]]
   {:http-xhrio {:method :get
                 :uri "/codelists"
                 :params {:dimension dimensions}
                 :response-format (ajax/transit-response-format)
                 :on-success [:http.codelists/success facet]
                 :on-failure [:http.codelists/error]}}))

;; HTTP RESPONSE HANDLERS

(rf/reg-event-db
 :http.codelists/success
 [e/validation-interceptor]
 (fn [db [_ facet response]]
   (let [status (if (empty? response) :success/empty :success/ready)
         updated-db (caching/cache-codelist db (:name facet) response)
         updated-facet (merge facet (-> updated-db :facets/config (get (:name facet))))]
     (-> updated-db
         (dissoc :facets.current/loading)
         (assoc :ui.facets.current/status status)
         (assoc :ui.facets/current updated-facet)))))

(rf/reg-event-db
 :http.codelists/error
 [e/validation-interceptor]
 (fn [db [_ error]]
   (assoc db :ui.facets.current/status :error)))
