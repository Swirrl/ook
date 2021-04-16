(ns ook.reframe.events.filter-ui
  (:require
   [re-frame.core :as rf]
   [ook.reframe.db :as db]
   [ook.reframe.db.selection :as selection]
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [ook.spec]
   [ook.reframe.events :as e]))

;;;;; FACETS

(rf/reg-event-fx
 :ui.facets/set-current
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ {:keys [codelists] :as facet}]]
   {:db (if codelists
          (let [with-ui-state (assoc facet :expanded #{})]
            (assoc db :ui.facets/current with-ui-state))
          db)
    :fx [(when-not codelists
           [:dispatch [:ui.facets.current/get-codelists facet]])]}))

(rf/reg-event-fx
 :ui.facets.current/get-codelists
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ facet]]
   {:db (assoc db :ui.facets.current/loading true)
    :fx [[:dispatch-later {:ms 300 :dispatch [:ui.facets.current/set-loading]}]
         [:dispatch [:facets.codelists/fetch-codelists facet]]]}))

(rf/reg-event-fx
 :filters/add-current-facet
 [e/validation-interceptor]
 (fn [{:keys [db]} _]
   (let [current-facet (:ui.facets/current db)
         selection (:selection current-facet)]
     {:db (cond-> db
            (seq selection) (assoc-in [:facets/applied (:name current-facet)] selection))
      :dispatch [:app/navigate :ook.route/search]})))

(rf/reg-event-db
 :ui.facets/cancel-current-selection
 [e/validation-interceptor]
 (fn [db _]
   (dissoc db :ui.facets/current)))

;;;;; SELECTING

(rf/reg-event-db
 :ui.facets.current/toggle-selection
 [e/validation-interceptor]
 (fn [db [_ option]]
   (selection/toggle db option)))

(rf/reg-event-db
 :ui.facets.current/set-selection
 [e/validation-interceptor]
 (fn [db [_ which {:keys [ook/uri] :as option}]]
   (condp = which
     :any (-> db (selection/add-codelist uri) (db/collapse-children uri))
     :add-children (selection/add-children db option)
     :remove-children (selection/remove-children db option))))

;;;;; EXPANDING/COLLAPSING

(rf/reg-event-db
 :ui.facets.current/toggle-expanded
 [e/validation-interceptor]
 (fn [db [_ uri]]
   (let [uri+children (cons uri (db/uri->expandable-child-uris db uri))
         expanded? (db/code-expanded? db uri)
         update-fn (if expanded? disj conj)]
     (update-in db [:ui.facets/current :expanded] #(apply update-fn % uri+children)))))

;;;;; HTTP

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

(rf/reg-event-fx
 :facets.codes/fetch-codes
 [e/validation-interceptor]
 (fn [_ [_ {:keys [ook/uri] :as codelist}]]
   {:http-xhrio {:method :get
                 :uri "/codes"
                 :params {:codelist uri}
                 :response-format (ajax/transit-response-format)
                 :on-success [:facets.codes/success codelist]
                 :on-failure [:facets.codes/error]}}))

(rf/reg-event-db
 :facets.codelists/success
 [e/validation-interceptor]
 (fn [db [_ facet-name result]]
   (let [old-facet (-> db :facets/config (get facet-name))
         codelists (sort-by :ook/uri result)
         facet-with-codelists (assoc old-facet :codelists codelists)]
     (-> db
         (dissoc :ui.facets.current/loading)
         ;; cache the result so we don't need to re-request it
         (assoc-in [:facets/config facet-name] facet-with-codelists)
         (assoc :ui.facets/current facet-with-codelists)
         (assoc-in [:ui.facets/current :expanded] #{})))))

(rf/reg-event-db
 :facets.codes/success
 [e/validation-interceptor]
 (fn [db [_ codelist result]]
   (let [current-facet (:ui.facets/current db)
         old-codelist (->> current-facet :codelists (filter #(= (:ook/uri %) (:ook/uri codelist))) first)
         codelists (->> current-facet :codelists (remove #(= (:ook/uri %) (:ook/uri codelist))))
         new-codelists (sort-by :ook/uri
                                (conj codelists (assoc old-codelist :children result)))
         facet-with-codelists (-> current-facet (assoc :codelists new-codelists))
         expanded (apply (fnil conj #{})
                         (:expanded current-facet)
                         (-> (:ook/uri codelist) (cons (db/all-expandable-uris result)) set))]
     (-> db
         ;; (update-in [db :ui.facets/current :codelists uri])
         ;; cache the result so we don't need to re-request it
         (assoc-in [:facets/config (:name current-facet)] (dissoc facet-with-codelists :selection :expanded))
         (assoc :ui.facets/current facet-with-codelists)
         (assoc-in [:ui.facets/current :expanded] expanded)))))

(rf/reg-event-db
 :ui.facets.current/error
 [e/validation-interceptor]
 (fn [db [_ error]]
   (assoc db :ui.facets/current :error)))
