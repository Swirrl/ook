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
 (fn [{:keys [db]} [_ {:keys [tree name] :as facet}]]
   {:db (let [with-ui-state (cond-> facet
                              :always (assoc :selection #{})
                              (seq tree) (assoc :expanded (db/all-expandable-uris tree)))]
          (assoc db :ui.facets/current with-ui-state))
    :fx [(when-not (:tree (db/facet-by-name db name))
           [:dispatch [:facets.codes/fetch-codes facet]])]}))

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
 :facets.codes/fetch-codes
 [e/validation-interceptor]
 (fn [_ [_ {:keys [name dimensions]}]]
   {:http-xhrio {:method :get
                 :uri "/codes"
                 :params {:dimension dimensions}
                 :response-format (ajax/transit-response-format)
                 :on-success [:facets.codes/success name]
                 :on-failure [:facets.codes/error]}}))

(rf/reg-event-db
 :facets.codes/success
 [e/validation-interceptor]
 (fn [db [_ facet-name result]]
   (let [old-facet (db/facet-by-name db facet-name)
         facet-with-tree (assoc old-facet :tree result)
         facets (->> db :facets/config (remove #(= (:name %) facet-name)))]
     (-> db
         (dissoc :facets.codes/error)
         ;; cache the result so we don't need to re-request it
         (assoc :facets/config (conj facets facet-with-tree))
         (assoc :ui.facets/current facet-with-tree)
         (assoc-in [:ui.facets/current :selection] #{})
         (assoc-in [:ui.facets/current :expanded] (db/all-expandable-uris result))))))

(rf/reg-event-db
 :facets.codes/error ;; TODO.. something in the ui if this actually happens
 [e/validation-interceptor]
 (fn [db [_ error]]
   (assoc db :facets.codes/error error)))
