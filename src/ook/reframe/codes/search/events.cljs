(ns ook.reframe.codes.search.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [ook.reframe.events :as e]))

(rf/reg-event-fx
  :ui.event/submit-code-search
  [e/validation-interceptor]
  (fn [{:keys [db]} _]
    (let [current-facet (:ui.facets/current db)]
      {:fx [[:dispatch [:http/search-codes current-facet]]]})))

(rf/reg-event-db
  :ui.event/search-term-change
  [e/validation-interceptor]
  (fn [db [_ new-search-term]]
    (assoc-in db [:ui.facets/current :search-term] new-search-term)))

;;; HTTP REQUESTS & RESPONSES

(rf/reg-event-fx
 :http/search-codes
 [e/validation-interceptor]
 (fn [_ [_ {:keys [search-term] :as facet}]]
   {:http-xhrio {:method :get
                 :uri "/code-search"
                 :params {:search-term search-term
                          :codelists (keys (:codelists facet))}
                 :response-format (ajax/transit-response-format)
                 :on-success [:http.codes.search/success facet]
                 :on-failure [:http.codes.search/error facet]}}))

(rf/reg-event-db
 :http.codes.search/success
 [e/validation-interceptor]
 (fn [db [_ facet results]]
   (let [updated-facet (assoc facet :codes.search/results results)]
     (-> db
         (assoc :ui.facets/current updated-facet)
         (assoc-in [:ui.facets/current :codes.search/status] :success)))))

(rf/reg-event-db
  :http.codes.search/error
  [e/validation-interceptor]
  (fn [db _]
    (assoc-in db [:ui.facets/current :codes.search/status] :error)))
