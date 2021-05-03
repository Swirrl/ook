(ns ook.reframe.codes.search.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [ook.reframe.codes.search.db :as db]
   [ook.reframe.events :as e]
   [ook.reframe.codes.db.disclosure :as disclosure]))

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

(rf/reg-event-fx
 :http.codes.search/success
 [e/validation-interceptor]
 (fn [db [_ facet results]]
   ;; (let [selection (db/code-result->selection results)

   ;;       ]
   ;;   )
   ;; show only codes in results, all expanded, if they're expandable
   ;; get relevant codelists so they can be shown

   (let [codelist-uris (->> results (map :scheme) set)
         current-facet (assoc (:ui.facets/current db) :selection (db/code-result->selection results))]
     {:fx [[:dispatch [:codes/get-concept-trees codelist-uris current-facet]]]})
   ;; (-> db
   ;;     (update-in [:ui.facets/current :expanded] disclosure/add-all-open-codes
   ;;                db
   ;;                (db/code-result->selection results)
   ;;                (:name facet))
   ;;     ;; (assoc-in [:ui.facets/current :selection])
   ;;     (assoc-in [:ui.facets/current :codes.search/status] :success))
   ))

(rf/reg-event-db
  :http.codes.search/error
  [e/validation-interceptor]
  (fn [db _]
    (assoc-in db [:ui.facets/current :codes/search :status] :error)))
