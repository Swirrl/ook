(ns ook.reframe.events.facets
  (:require
   [re-frame.core :as rf]
   [ook.reframe.db :as db]
   [ook.reframe.db.caching :as caching]
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [ook.spec]
   [ook.reframe.events :as e]))


;; UI MANAGEMENT

(rf/reg-event-fx
 :ui.facets/set-current
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ {:keys [codelists] :as facet}]]
   ;; (if codelists
   ;;   (let [])
   ;;   {:db (let [with-ui-state (assoc facet :expanded #{})])
   ;;    (assoc db :ui.facets/current with-ui-state)})
   ;; if data already fetched --
   ;;    set the cached data and apply the old facet
   ;; else
   ;;    fetch teh new data, cache it, set it, and also apply the old facet

   (if codelists
     {:db (assoc db :ui.facets/current facet)}
     {:fx [;; [:dispatch [:filters/add-current-facet]]
         ;; TODO -- not the right solution -- rearchitect to decouple getting current facet and applying it@
           [:dispatch [:ui.facets.current/get-codelists facet]]]})

   ;; {:db (if codelists
   ;;        (assoc db :ui.facets/current facet)
   ;;        db)
   ;;  :fx [;; [:dispatch [:filters/add-current-facet]]
   ;;       ;; TODO -- not the right solution -- rearchitect to decouple getting current facet and applying it@
   ;;       (when-not codelists
   ;;         [:dispatch [:ui.facets.current/get-codelists facet]])]}
   ))

(rf/reg-event-fx
 :ui.facets.current/get-codelists
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ facet]]
   {:db (assoc db :ui.facets.current/loading true)
    :fx [[:dispatch-later {:ms 300 :dispatch [:ui.facets.current/set-loading]}]
         [:dispatch [:facets.codelists/fetch-codelists facet]]]}))

(rf/reg-event-db
 :ui.facets.current/set-loading
 [e/validation-interceptor]
 (fn [db _]
   (if (:ui.facets.current/loading db)
     (assoc db :ui.facets/current :loading)
     db)))

(rf/reg-event-db
 :ui.facets/cancel-current-selection
 [e/validation-interceptor]
 (fn [db _]
   (dissoc db :ui.facets/current)))

(rf/reg-event-fx
 :ui.filters/apply-facet
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ {:keys [name selection]}]]
   {:db (cond-> db
          (seq selection) (assoc-in [:facets/applied name] selection))
    :dispatch [:app/navigate :ook.route/search]}))

;; HTTP REQUEST

(rf/reg-event-fx
 :facets.codelists/fetch-codelists
 [e/validation-interceptor]
 (fn [_ [_ {:keys [name dimensions]}]]
   {:http-xhrio {:method :get
                 :uri "/codelists"
                 :params {:dimension dimensions}
                 :response-format (ajax/transit-response-format)
                 :on-success [:facets.codelists/success name]
                 :on-failure [:ui.facets.current/error]}}))

;; HTTP RESPONSE HANDLERS

(rf/reg-event-db
 :facets.codelists/success
 [e/validation-interceptor]
 (fn [db [_ facet-name result]]
   (let [updated-db (caching/cache-codelist db facet-name result)]
     (-> updated-db
         (dissoc :ui.facets.current/loading)
         (assoc :ui.facets/current (-> updated-db :facets/config (get facet-name)))))))

(rf/reg-event-db
 :ui.facets.current/error
 [e/validation-interceptor]
 (fn [db [_ error]]
   (assoc db :ui.facets/current :error)))
