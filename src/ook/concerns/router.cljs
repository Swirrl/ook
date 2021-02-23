(ns ook.concerns.router
  (:require [ook.ui.search :as search]
            [ook.ui.state :as state]
            [reitit.frontend :as rf]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe]
            [ook.handler :as handler]))

(def ^:private routes
  [["/" {:name :ook.route/home
         :controllers [{:start search/reset-state!}]}]
   ["/search" {:name :ook.route/search
               :parameters {:query {:q string?
                                    :code [string?]}}
               :controllers [{:start (fn [params]
                                       (let [query (-> params :query :q)
                                             ui (-> @state/components-state :main :ui :codes :query)]
                                         ;; happens if you navigate forward/backward in the browser
                                         (when-not (= query ui)
                                           (handler/get-code-search-results query))))
                              :parameters {:query [:q]}}]}]])

(defn- handle-navigation [new-match]
  (swap! state/match (fn [old-match]
                       ;; (println "changing from::: " (-> old-match :data :name))
                       ;; (println "changing to::: " (-> new-match :data :name))
                       (when new-match
                         (assoc new-match
                                :controllers
                                (rfc/apply-controllers (:controllers old-match) new-match))))))

(defn init! []
  (rfe/start! (rf/router routes
                         ;; don't use coercion, there's a bug for multiple values
                         ;; {:data {:coercion rss/coercion}}
                         )
              handle-navigation
              {:use-fragment false}))
