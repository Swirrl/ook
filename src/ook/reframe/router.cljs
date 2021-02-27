(ns ook.reframe.router
  (:require
   [ook.reframe.views :as views]
   [re-frame.core :as rf]
   [reitit.frontend.controllers :as rtfc]
   [reitit.frontend.easy :as rtfe]
   [reitit.frontend :as rt]))

(def ^:private routes
  [["/" {:name :ook.route/home
         :view views/home
         :controllers [{:start #(rf/dispatch [:init/initialize-db])}]}]

   ["/search" {:name :ook.route/search
               :view views/results
               :parameters {:query {:q string? :code [string?]}}
               :controllers [{:start (fn [params]
                                       (let [query (-> params :query :q)
                                             codes (-> params :query :code)]
                                         (rf/dispatch [:codes/submit-search query])
                                         (when codes
                                           (rf/dispatch [:filters/apply-code-selection codes]))))
                              :parameters {:query [:q :code]}}]}]])

(defn- handle-navigation [new-match]
  (let [old-match @(rf/subscribe [:app/current-route])]
    (when new-match
      (let [controllers (rtfc/apply-controllers (:controllers old-match) new-match)
            match (assoc new-match :controllers controllers)]
        (rf/dispatch [:app/navigated match])))))

(defn init! []
  (println "Initializing router...")
  (rtfe/start! (rt/router routes
                          ;; don't use coercion, there's a bug for multiple values
                          ;; {:data {:coercion rss/coercion}}
                          )
               handle-navigation
               {:use-fragment false}))
