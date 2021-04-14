(ns ook.reframe.router
  (:require
   [ook.reframe.views :as views]
   [re-frame.core :as rf]
   [reitit.frontend.controllers :as rtfc]
   [reitit.frontend.easy :as rtfe]
   [reitit.frontend :as rt]))

(defn home-controller []
  (rf/dispatch [:datasets/fetch-datasets nil]))

(def home-route-data
  {:name :ook.route/home
   :view views/search
   :controllers [{:start home-controller}]})

(defn search-controller [params]
  (let [filter-state (-> params :query :filters)]
    (if filter-state
      (rf/dispatch [:filters/apply filter-state])
      (rf/dispatch [:app/navigate :ook.route/home]))))

(def ^:private routes
  [["/" home-route-data]
   ["/search" {:name :ook.route/search
               :view views/search
               :parameters {:query {:filters [string?]}}
               :controllers [{:start search-controller
                              :parameters {:query [:filters]}}]}]])

(defn- handle-navigation [new-match]
  (let [old-match @(rf/subscribe [:app/current-route])]
    (when new-match
      (let [controllers (rtfc/apply-controllers (:controllers old-match) new-match)
            match (assoc new-match :controllers controllers)]
        (rf/dispatch [:app/navigated match])))))

(defn init! []
  (println "Initializing router...")
  (rtfe/start! (rt/router routes
                          ;; don't use coercion, there's a bug for single-item lists
                          ;; https://github.com/metosin/reitit/issues/298
                          ;; {:data {:coercion rss/coercion}}
                          )
               handle-navigation
               {:use-fragment false}))
