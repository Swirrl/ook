(ns ook.handler
  (:require [ook.ui.state :as state]
            [ajax.core :refer [GET transit-response-format]]
            [reitit.frontend.easy :as rfe]))

(defn- navigate [route query-params]
  (rfe/push-state route {} query-params))

(defn- get-search-results [query]
  (GET "/search"
    {:params {:q query}
     :response-format (transit-response-format)
     :handler  (fn [result]
                 (swap! state/components-state assoc :search result)
                 (navigate :ook.route/search {:q query}))
     :error-handler (fn [error]
                      (println "Error fetching search results!: " error)
                      (swap! state/components-state assoc :search :error))}))

(defn submit-search [event]
  (.preventDefault event)
  (let [query (-> event .-target js/FormData. (.get "q"))]
    (get-search-results query)))
