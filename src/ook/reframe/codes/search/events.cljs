(ns ook.reframe.codes.search.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [ook.reframe.codes.search.db :as search]
   [ook.reframe.events :as e]
   [ook.reframe.codes.db.disclosure :as disclosure]
   [ook.reframe.facets.db :as facets]))

;;; USER INITIATED EVENT HANDLERS

(rf/reg-event-fx
 :ui.event/submit-code-search
 [e/validation-interceptor]
 (fn [{:keys [db]} _]
   {:async-flow
    {:first-dispatch [:http/search-codes (:ui.facets/current db)]
     :rules [{:when :seen? :events :http.codes.search/success
              :dispatch-fn (fn [[_  _ results]]
                             [[:ui.codes.search/set-facet-ui results]
                              [:ui.codes.search/get-missing-code-trees results]])}]}}))

(rf/reg-event-db
 :ui.event/reset-search
 (fn [db _]
   (update-in db [:ui.facets/current] dissoc :codes/search :expanded)))

;;; UI MANAGEMENT

(rf/reg-event-db
 :ui.codes.search/set-facet-ui
 [e/validation-interceptor]
 (fn [db [_ results]]
   (let [facet-name (-> db :ui.facets/current :name)
         selection (search/code-result->selection results)]
     (-> db
         (assoc-in [:ui.facets/current :expanded] #{})
         (update-in [:ui.facets/current :expanded]
                    disclosure/expand-all-selected-codes db selection facet-name)))))

(rf/reg-event-fx
 :ui.codes.search/get-missing-code-trees
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ results]]
   (let [facet-name (-> db :ui.facets/current :name)
         facet-ui (search/set-selection-and-disclosure db facet-name results)]
     {:fx [[:dispatch [:ui.codes/get-all-code-trees-with-selections facet-ui]]]})))

(rf/reg-event-db
 :ui.event/search-term-change
 [e/validation-interceptor]
 (fn [db [_ new-search-term]]
   (assoc-in db [:ui.facets/current :codes/search :search-term] new-search-term)))

;;; HTTP REQUESTS & RESPONSES

(rf/reg-event-fx
 :http/search-codes
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ {:keys [codes/search name]}]]
   (let [codelists (map :ook/uri (facets/get-codelists db name))]
     {:http-xhrio {:method :get
                   :uri "/code-search"
                   :params {:search-term (:search-term search)
                            :codelists codelists}
                   :response-format (ajax/transit-response-format)
                   :on-success [:http.codes.search/success name]
                   :on-failure [:http.codes.search/error]}})))

(rf/reg-event-db
 :http.codes.search/success
 [e/validation-interceptor]
 (fn [db [_ facet-name results]]
   (-> db
       (assoc-in [:ui.facets/current :codes/search :status] :ready)
       (assoc-in [:ui.facets/current :codes/search :results] results))))

(rf/reg-event-db
 :http.codes.search/error
 [e/validation-interceptor]
 (fn [db _]
   (assoc-in db [:ui.facets/current :codes/search :status] :error)))
