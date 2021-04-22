(ns ook.reframe.codes.events
  (:require
   [re-frame.core :as rf]
   [ook.reframe.codes.db.caching :as caching]
   [ajax.core :as ajax]
   [ook.reframe.db :as db]
   [ook.reframe.codes.db.selection :as selection]
   [ook.reframe.events :as e]))

;;; CLICK HANDLERS

(rf/reg-event-fx
 :ui.event/apply-current-facet
 [e/validation-interceptor]
 (fn [{:keys [db]} _]
   (let [current-facet (:ui.facets/current db)]
     {:db (dissoc db :ui.facets/current)
      :dispatch [:facets/apply-facet current-facet]})))

(rf/reg-event-db
 :ui.event/toggle-disclosure
 [e/validation-interceptor]
 (fn [db [_ uri]]
   (let [uri+children (cons uri (db/uri->expandable-child-uris db uri))
         expanded? (db/code-expanded? db uri)
         update-fn (if expanded? disj (fnil conj #{}))]
     (update-in db [:ui.facets/current :expanded] #(apply update-fn % uri+children)))))

(rf/reg-event-fx
 :ui.event/get-codes
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ facet codelist-uri]]
   {:db (-> db
            (update-in [:ui.facets/current :expanded] conj codelist-uri)
            (assoc-in [:ui.facets.current.codes/loading codelist-uri] true))
    :fx [[:dispatch-later {:ms 100 :dispatch [:ui.facets.current.codes/set-loading codelist-uri]}]
         [:dispatch [:http/fetch-codes facet codelist-uri]]]}))

(rf/reg-event-db
  :ui.event/set-selection
 [e/validation-interceptor]
 (fn [db [_ which {:keys [ook/uri] :as option}]]
   (condp = which
     :any (-> db (selection/add-codelist uri) (db/collapse-children uri))
     :add-children (selection/add-children db option)
     :remove-children (selection/remove-children db option))))

(rf/reg-event-db
 :ui.event/toggle-selection
 [e/validation-interceptor]
 (fn [db [_ option]]
   (selection/toggle db option)))

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
 :http.codes/error
 [e/validation-interceptor]
 (fn [db [_ codelist-uri]]
   (assoc-in db [:ui.facets/current :codelists codelist-uri :children] :error)))
