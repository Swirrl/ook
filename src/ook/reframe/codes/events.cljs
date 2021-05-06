(ns ook.reframe.codes.events
  (:require
   [re-frame.core :as rf]
   [ook.reframe.codes.db.caching :as caching]
   [ajax.core :as ajax]
   [ook.reframe.codes.db.selection :as selection]
   [ook.reframe.codes.db.disclosure :as disclosure]
   [ook.reframe.events :as e]))

;;; CLICK HANDLERS

(rf/reg-event-fx
 :ui.event/apply-current-facet
 [e/validation-interceptor]
 (fn [{:keys [db]} _]
   (let [current-facet (:ui.facets/current db)]
     {:db (dissoc db :ui.facets/current)
      :fx [[:dispatch [:ui.event/cancel-current-selection]]
           [:dispatch [:facets/apply-facet current-facet]]]})))

(rf/reg-event-db
 :ui.event/toggle-disclosure
 [e/validation-interceptor]
 (fn [db [_ uri]]
   (update db :ui.facets/current disclosure/toggle uri)))

(rf/reg-event-fx
 :ui.event/toggle-codelist
 [e/validation-interceptor]
 (fn [_ [_ uri]]
   {:fx [[:dispatch [:ui.event/toggle-disclosure uri]]
         [:dispatch [:codes/get-codes uri]]]}))

(rf/reg-event-db
 :ui.event/set-selection
 [e/validation-interceptor]
 (fn [db [_ which {:keys [ook/uri] :as option}]]
   (condp = which
     :any (-> db
              (update :ui.facets/current selection/add-codelist uri)
              (update :ui.facets/current disclosure/collapse uri))
     :add-children (-> db
                       (update :ui.facets/current selection/add-children option)
                       (update :ui.facets/current disclosure/expand uri))
     :remove-children (update db :ui.facets/current selection/remove-children option))))

(rf/reg-event-db
 :ui.event/toggle-selection
 [e/validation-interceptor]
 (fn [db [_ option]]
   (update db :ui.facets/current selection/toggle option)))

;;; UI MANAGEMENT

(rf/reg-event-fx
 :ui.codes/get-all-code-trees-with-selections
 [e/validation-interceptor]
 (fn [_ [_ {:keys [selection] :as facet-ui}]]
   (let [codelist-uris (disclosure/open-codelist-uris selection)]
     {:fx (for [codelist-uri codelist-uris]
            [:dispatch [:ui.codes/get-code-tree-open-to-selection facet-ui codelist-uri]])})))

(rf/reg-event-fx
 :ui.codes/get-code-tree-open-to-selection
 [e/validation-interceptor]
 (fn [_ [_ facet-ui codelist-uri]]
   {:async-flow
    {:first-dispatch [:codes/get-codes codelist-uri]
     :rules [{:when :seen? :events (fn [[event _facet finished-codelist-uri]]
                                     (and (= event :http.codes/success)
                                          (= codelist-uri finished-codelist-uri)))
              :dispatch [:ui.codes/expand-codelist-to-selection facet-ui codelist-uri]}]}}))

(rf/reg-event-db
  :ui.codes/expand-codelist-to-selection
  [e/validation-interceptor]
  (fn [db [_ {:keys [selection name]} codelist-uri]]
    (update-in db [:ui.facets/current :expanded]
               disclosure/expand-selected-codes-for-codelist db selection name codelist-uri)))


(rf/reg-event-fx
 :codes/get-codes
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ codelist-uri]]
   (let [facet-name (-> db :ui.facets/current :name)]
     (when-not (caching/concept-tree-cached? db facet-name codelist-uri)
       {:fx [[:dispatch-later {:ms 300 :dispatch [:ui.facets.current.codes/set-loading codelist-uri]}]
             [:dispatch [:http/fetch-codes facet-name codelist-uri]]]}))))

(rf/reg-event-db
 :ui.facets.current.codes/set-loading
 [e/validation-interceptor]
 (fn [db [_ codelist-uri]]
   (let [status (get-in db [:ui.codes/status codelist-uri])]
     (if (#{:ready :error} status)
       db
       (assoc-in db [:ui.codes/status codelist-uri] :loading)))))

;;; HTTP REQUESTS & RESPONSES

(rf/reg-event-fx
 :codes/get-concept-trees
 [e/validation-interceptor]
 (fn [_db [_ codelist-uris facet]]
   {:fx (for [codelist-uri codelist-uris]
          [:dispatch [:codes/get-codes codelist-uri facet]])}))

(rf/reg-event-fx
 :http/fetch-codes
 [e/validation-interceptor]
 (fn [_ [_ facet-name codelist-uri]]
   {:http-xhrio {:method :get
                 :uri "/codes"
                 :params {:codelist codelist-uri}
                 :response-format (ajax/transit-response-format)
                 :on-success [:http.codes/success facet-name codelist-uri]
                 :on-failure [:http.codes/error codelist-uri]}}))

(rf/reg-event-db
 :http.codes/success
 [e/validation-interceptor]
 (fn [db [_ facet-name codelist-uri result]]
   (let [children (if (seq result) result :no-children)
         updated-db (caching/cache-code-tree db facet-name codelist-uri children)]
     (update-in updated-db [:ui.codes/status] assoc codelist-uri :ready))))

(rf/reg-event-db
 :http.codes/error
 [e/validation-interceptor]
 (fn [db [_ codelist-uri]]
   (assoc-in db [:ui.codes/status codelist-uri] :error)))
