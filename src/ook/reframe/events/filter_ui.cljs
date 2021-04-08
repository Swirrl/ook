(ns ook.reframe.events.filter-ui
  (:require
   [re-frame.core :as rf]
   [ook.reframe.db :as db]
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [ook.spec]
   [ook.reframe.events :as e]))

;;;;; FACETS

(rf/reg-event-fx
 :ui.facets/set-current
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ {:keys [tree] :as facet}]]
   {:db (if tree
          (let [with-ui-state (assoc facet :selection #{} :expanded #{})]
            (assoc db :ui.facets/current with-ui-state))
          db)
    :fx [(when-not tree
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
         selection (seq (:selection current-facet))]
     {:db (cond-> db
            selection (assoc-in [:facets/applied (:name current-facet)] selection))
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
 (fn [db [_ uri]]
   (let [selected? (db/code-selected? db uri)
         update-fn (if selected? disj conj)]
     (update-in db [:ui.facets/current :selection] update-fn uri))))

(rf/reg-event-db
 :ui.facets.current/set-selection
 [e/validation-interceptor]
 (fn [db [_ which uri]]
   (let [to-add (cond-> (db/uri->child-uris db uri)
                  (= :any which) (conj uri))
         to-collapse (when (= :any which) (cons uri (db/uri->expandable-child-uris db uri)))]
     (-> db
         (update-in [:ui.facets/current :selection] #(apply conj % to-add))
         (update-in [:ui.facets/current :expanded] #(apply disj % to-collapse))))))

;;;;; EXPANDING/COLLAPSING

(rf/reg-event-fx
 :ui.facets.current/toggle-codelist
 [e/validation-interceptor]
 (fn [{:keys [db]} [_ {:keys [children ook/uri] :as codelist}]]
   {:fx [(if (seq children)
           [:dispatch [:ui.facets.current/toggle-expanded uri]]
           [:dispatch [:facets.codes/fetch-codes codelist]])]}))

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
 (fn [{:keys [db]} [_ {:keys [ook/uri] :as codelist}]]
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
   (let [old-facet (db/facet-by-name db facet-name)
         codelists (map #(assoc % :allow-any? true :children []) result)
         facet-with-tree (assoc old-facet :tree codelists)
         facets (->> db :facets/config (remove #(= (:name %) facet-name)))]
     (-> db
         (dissoc :ui.facets.current/loading)
         ;; cache the result so we don't need to re-request it
         (assoc :facets/config (conj facets facet-with-tree))
         (assoc :ui.facets/current facet-with-tree)
         (assoc-in [:ui.facets/current :selection] #{})
         (assoc-in [:ui.facets/current :expanded] #{})))))

(rf/reg-event-db
 :facets.codes/success
 [e/validation-interceptor]
 (fn [db [_ codelist result]]
   (let [old-facet (-> db :ui.facets/current (dissoc :selection :expanded))
         old-codelist (->> old-facet :tree (filter #(= (:ook/uri %) (:ook/uri codelist))) first)
         tree (->> old-facet :tree (remove #(= (:ook/uri %) (:ook/uri codelist))))
         facet-with-tree (-> old-facet
                             (assoc :tree (conj tree (assoc old-codelist :children result))))
         facets (->> db :facets/config (remove #(= (:name %) (:name old-facet))))
         expanded (-> (:ook/uri codelist) (cons (db/all-expandable-uris result)) set)]
     (-> db
         ;; cache the result so we don't need to re-request it
         (assoc :facets/config (conj facets facet-with-tree))
         (assoc :ui.facets/current facet-with-tree)
         (assoc-in [:ui.facets/current :selection] #{})
         (assoc-in [:ui.facets/current :expanded] expanded)))))

(rf/reg-event-db
  :ui.facets.current/error
 [e/validation-interceptor]
 (fn [db [_ error]]
   (assoc db :ui.facets/current :error)))

(rf/reg-event-db
 :ui.facets.current/set-loading
 [e/validation-interceptor]
 (fn [db _]
   (if (:ui.facets.current/loading db)
     (assoc db :ui.facets/current :loading)
     db)))
