(ns ook.reframe.events.codes
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
 :facets.codes/get-codes
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ facet codelist-uri]]
   {:db (-> db
            (update-in [:ui.facets/current :expanded] conj codelist-uri)
            (assoc-in [:ui.facets.current.codes/loading codelist-uri] true))
    :fx [[:dispatch-later {:ms 200 :dispatch [:ui.facets.current.codes/set-loading codelist-uri]}]
         [:dispatch [:facets.codes/fetch-codes facet codelist-uri]]]}))

(rf/reg-event-db
 :ui.facets.current.codes/set-loading
 [e/validation-interceptor]
 (fn [db [_ codelist-uri]]
   (if (get-in db [:ui.facets.current.codes/loading codelist-uri])
     (assoc-in db [:ui.facets/current :codelists codelist-uri :children] :loading)
     db)))

;; HTTP REQUEST

(rf/reg-event-fx
 :facets.codes/fetch-codes
 [e/validation-interceptor]
 (fn [_ [_ facet codelist-uri]]
   {:http-xhrio {:method :get
                 :uri "/codes"
                 :params {:codelist codelist-uri}
                 :response-format (ajax/transit-response-format)
                 :on-success [:facets.codes/success facet codelist-uri]
                 :on-failure [:facets.codes/error codelist-uri]}}))

;; HTTP RESPONSE HANDLER

(rf/reg-event-db
 :facets.codes/success
 [e/validation-interceptor]
 (fn [db [_ facet codelist-uri result]]
   (let [children (if (seq result) result :no-children)
         updated-db (caching/cache-code-tree db (:name facet) codelist-uri children)
         expanded (apply (fnil conj #{})
                         (:expanded facet)
                         (-> codelist-uri (cons (db/all-expandable-uris result)) set))
         facet-with-ui-state (-> updated-db :facets/config (get (:name facet))
                                 (assoc :expanded expanded)
                                 (assoc :selection (:selection facet)))]
     (-> updated-db
         (update :ui.facets.current.codes/loading dissoc codelist-uri)
         (assoc :ui.facets/current facet-with-ui-state)))))

(rf/reg-event-db
  :facets.codes/error
  [e/validation-interceptor]
  (fn [db [_ codelist-uri]]
    (assoc-in db [:ui.facets/current :codelists codelist-uri :children] :error)))
