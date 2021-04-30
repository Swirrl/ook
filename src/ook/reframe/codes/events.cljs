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
     {:fx [[:dispatch [:ui.event/toggle-disclosure uri]]
           (when-not (caching/concept-tree-cached? db facet-name uri)
             [:dispatch [:codes/get-codes uri]])]})))

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

(rf/reg-event-db
 :ui.facets.current.codes/set-loading
 [e/validation-interceptor]
 (fn [db [_ codelist-uri]]
   (if (get-in db [:ui.facets.current.codes/loading codelist-uri])
     (assoc-in db [:ui.facets/current :codelists codelist-uri :children] :loading)
     db)))

;;; HTTP REQUESTS & RESPONSES

(rf/reg-event-fx
 :codes/get-codes
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ codelist-uri specific-facet]]
   (let [facet (or specific-facet (:ui.facets/current db))]
     {:db (assoc-in db [:ui.facets.current.codes/loading codelist-uri] true)
      :fx [[:dispatch-later {:ms 100 :dispatch [:ui.facets.current.codes/set-loading codelist-uri]}]
           [:dispatch [:http/fetch-codes facet codelist-uri]]]})))

(rf/reg-event-fx
 :codes/get-selected-concept-trees
 [e/validation-interceptor]
 (fn [_db [_ facet]]
   (let [codelist-uris (->> facet :selection disclosure/open-codelist-uris)]
     {:fx (for [codelist-uri codelist-uris]
            [:dispatch [:codes/get-codes codelist-uri facet]])})))

(rf/reg-event-fx
 :http/fetch-codes
 [e/validation-interceptor]
 (fn [_ [_ facet codelist-uri]]
   {:http-xhrio {:method :get
                 :uri "/codes"
                 :params {:codelist codelist-uri}
                 :response-format (ajax/transit-response-format)
                 :on-success [:http.codes/success facet codelist-uri]
                 :on-failure [:http.codes/error codelist-uri]}}))

(rf/reg-event-db
 :http.codes/success
 [e/validation-interceptor]
 (fn [db [_ {:keys [selection name] :as facet} codelist-uri result]]
   (let [children (if (seq result) result :no-children)
         updated-db (caching/cache-code-tree db (:name facet) codelist-uri children)
         updated-facet (-> facet
                           (merge (get-in updated-db [:facets/config name]))
                           (update :expanded disclosure/add-all-open-codes updated-db selection name))]
     (-> updated-db
         (update :ui.facets.current.codes/loading dissoc codelist-uri)
         (assoc :ui.facets/current updated-facet)))))

(rf/reg-event-db
 :http.codes/error
 [e/validation-interceptor]
 (fn [db [_ codelist-uri]]
   (assoc-in db [:ui.facets/current :codelists codelist-uri :children] :error)))
