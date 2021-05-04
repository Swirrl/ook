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
 (fn [{:keys [db]} [_ uri]]
   (let [facet-name (-> db :ui.facets/current :name)]
     {:fx [(if (caching/concept-tree-cached? db facet-name uri)
             [:dispatch [:ui.event/toggle-disclosure uri]]
             [:dispatch [:codes/get-codes-flow uri]])]})))

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
 :codes/get-codes
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ codelist-uri specific-facet]]
   (let [facet-name (:name (or specific-facet (:ui.facets/current db)))]
     {:db (-> db
              (assoc-in [:ui.codes/status codelist-uri] :delay)
              (update :ui.codes/currently-loading (fnil conj #{}) codelist-uri))
      :fx [[:dispatch-later {:ms 100 :dispatch [:ui.facets.current.codes/set-loading codelist-uri]}]
           [:dispatch [:http/fetch-codes facet-name codelist-uri]]]})))

(rf/reg-event-db
 :ui.facets.current.codes/set-loading
 [e/validation-interceptor]
 (fn [db [_ codelist-uri]]
   (if (get-in db [:ui.codes/currently-loading codelist-uri])
     (assoc-in db [:ui.codes/status codelist-uri] :loading)
     db)))

(defn get-codes-flow [codelist-uri]
  {:first-dispatch [:codes/get-codes codelist-uri]
   :rules [{:when :seen? :events :codes/get-codes :dispatch [:ui.event/toggle-disclosure codelist-uri]}
           {:when :seen? :events :http.codes/success :halt? true
            :dispatch-fn (fn [[_ facet-ui codelist-uri]]
                           [[:ui.codes/expand-selected-codes facet-ui codelist-uri]])}
           {:when :seen? :events :http.codes/error :halt? true}]})

(rf/reg-event-fx
 :codes/get-codes-flow
 [e/validation-interceptor]
 (fn [_ [_ codelist-uri]]
   {:async-flow (get-codes-flow codelist-uri)}))

(rf/reg-event-db
 :ui.codes/expand-selected-codes
 [e/validation-interceptor]
 (fn [db [_ {:keys [selection name]}]]
   (update-in db [:ui.facets/current :expanded]
              disclosure/expand-selected-codes db selection name)))

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
     (-> updated-db
         (update :ui.codes/currently-loading disj codelist-uri)
         (update-in [:ui.codes/status] assoc codelist-uri :ready)))))

(rf/reg-event-db
 :http.codes/error
 [e/validation-interceptor]
 (fn [db [_ codelist-uri]]
   (assoc-in db [:ui.codes/status codelist-uri] :error)))
