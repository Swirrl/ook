(ns ook.reframe.events.filter-ui
  (:require
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [re-frame.core :as rf]
   [ook.reframe.db :as db]
   [ook.reframe.db.selection :as selection]
   [day8.re-frame.http-fx]
   [ook.reframe.events :as e]))

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
 (fn-traced [db [_ uri]]
   (let [uri+children (cons uri (db/uri->expandable-child-uris db uri))
         expanded? (db/code-expanded? db uri)
         update-fn (if expanded? disj (fnil conj #{}))]
     (update-in db [:ui.facets/current :expanded] #(apply update-fn % uri+children)))))
