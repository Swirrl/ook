(ns ook.reframe.router
  (:require
   [ook.reframe.views :as views]
   [re-frame.core :as rf]
   [reitit.frontend.controllers :as rtfc]
   [reitit.frontend.easy :as rtfe]
   [reitit.frontend :as rt]))

(def ^:private routes
  [["/" {:name :ook.route/home
         :view views/search
         :controllers [{:start #(rf/dispatch [:init/initialize-db])}]}]

   ;; ["/search" {:name :ook.route/search
   ;;             :view views/results
   ;;             :parameters {:query {:q string? :facet [string?]}}
   ;;             :controllers [{:start (fn [params]
   ;;                                     (let [query (-> params :query :q)
   ;;                                           facets (-> params :query :facet)]
   ;;                                       (rf/dispatch [:codes/submit-search query])
   ;;                                       (when facets
   ;;                                         (rf/dispatch [:filters/apply-code-selection facets]))))
   ;;                            :parameters {:query [:q :facet]}}]}]

   ["/search" {:name :ook.route/search
               :view views/search
               :parameters {:query {:facet [string?]}}
               :controllers [{:start (fn [params]
                                       (let [facets (-> params :query :facet)]
                                         (if facets
                                           (rf/dispatch [:filters/apply facets])
                                           (rf/dispatch [:init/initialize-db]))))
                              :parameters {:query [:facet]}}]}]])

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
