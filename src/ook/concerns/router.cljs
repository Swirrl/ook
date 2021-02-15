(ns ook.concerns.router
  (:require [ook.ui.search :as search]
            [ook.ui.state :as state]
            [reitit.frontend :as rf]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe]))

(def ^:private routes
  [["/" {:name :ook.route/home
         :view search/search-form
         :controllers [{:start #(swap! state/components-state assoc :search nil)
                        :stop (fn [s] (js/console.log "stop / :::" s))
                        :identity :data}]}]
   ["/search" {:name  :ook.route/search
               :view search/ui
               :parameters {:query {:q string?}}
               :controllers [{:start (fn [s] (js/console.log "stop / :::" s))
                              :identity :data}]}]])

(defn- handle-navigation [new-match]
  (swap! state/match (fn [old-match]
                       (println "changing from::: " (-> old-match :data))
                       (println "changing to::: " (-> new-match :data))
                       (when new-match
                         (assoc new-match
                                :controllers
                                (rfc/apply-controllers (:controllers old-match) new-match))))))

(defn init! []
  (rfe/start! (rf/router routes) handle-navigation {:use-fragment false}))
