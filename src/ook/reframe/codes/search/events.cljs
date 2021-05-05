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
   {:fx [[:dispatch-later {:ms 300 :dispatch [:ui.codes.search/set-loading]}]]
    :async-flow
    {:first-dispatch [:http/search-codes (:ui.facets/current db)]
     :rules [{:when :seen? :events :http.codes.search/success
              :dispatch-fn (fn [[_ _ results]]
                             [[:ui.codes.search/set-facet-ui results]])}
             {:when :seen? :events :http.codes.search/success
              :dispatch-fn (fn [[_ _ results]]
                             [[:ui.codes.search/get-missing-code-trees results]])}]}}))

(rf/reg-event-db
 :ui.event/reset-search
 (fn [db _]
   (update-in db [:ui.facets/current] dissoc :codes/search)))

;;; UI MANAGEMENT

(rf/reg-event-db
 :ui.codes.search/set-loading
 [e/validation-interceptor]
 (fn [db _]
   (let [status (get-in db [:ui.facets/current :codes/search :status])]
     (if (= status :ready)
       db
       (assoc-in db [:ui.facets/current :codes/search :status] :loading)))))


(rf/reg-event-db
 :ui.codes.search/set-facet-ui
 [e/validation-interceptor]
 (fn [db [_ results]]
   (let [selection (search/code-result->selection results)
         open-codelists (disclosure/open-codelist-uris selection)
         open-codes (->> results (filter :children?) (map :ook/uri))
         expanded (set (concat open-codelists open-codes))]
     (assoc-in db [:ui.facets/current :expanded] expanded))))

(rf/reg-event-fx
 :ui.codes.search/get-missing-code-trees
 [e/validation-interceptor]
 (fn []
   {}))

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
   ;; (let [selection (search/code-result->selection results)

   ;;       ]
   ;;   )
   ;; show only codes in results, all expanded, if they're expandable
   ;; get relevant codelists so they can be shown
   (-> db
       (assoc-in [:ui.facets/current :codes/search :status] :ready)
       (assoc-in [:ui.facets/current :codes/search :results] results))

   ;; (let [codelist-uris (->> results (map :scheme) set)
   ;;       current-facet (assoc (:ui.facets/current db) :selection (search/code-result->selection results))]
   ;;   {:fx [[:dispatch [:codes/get-concept-trees codelist-uris current-facet]]]})
   ;; (-> db
   ;;     (update-in [:ui.facets/current :expanded] disclosure/add-all-open-codes
   ;;                db
   ;;                (search/code-result->selection results)
   ;;                (:name facet))
   ;;     ;; (assoc-in [:ui.facets/current :selection])
   ;;     (assoc-in [:ui.facets/current :codes.search/status] :success))
   ))

(rf/reg-event-db
 :http.codes.search/error
 [e/validation-interceptor]
 (fn [db _]
   (assoc-in db [:ui.facets/current :codes/search :status] :error)))
