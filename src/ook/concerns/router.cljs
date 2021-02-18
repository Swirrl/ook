(ns ook.concerns.router
  (:require [ook.ui.search :as search]
            [ook.ui.state :as state]
            [reitit.frontend :as rf]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe]))

(def ^:private routes
  [["/" {:name :ook.route/home
         :controllers [{:start search/reset-search-input!}]}]
   ["/search" {:name  :ook.route/search
               :parameters {:query {:q string?}}}]])

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
