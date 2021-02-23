(ns ook.handler
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [ajax.core :refer [GET transit-response-format]]
            [cljs.core.async :as a]
            [ook.ui.state :as state]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfe]))

(def code-search-requests (a/chan))
(def dataset-search-requests (a/chan))

(defn get-code-search-results [query]
  (GET "/search"
    {:params {:q query}
     :response-format (transit-response-format)
     :handler (fn [result] (go (a/>! code-search-requests result)))
     :error-handler (fn [error] (go (a/>! code-search-requests {:error error})))}))

(defn- update-codes-state [response]
  (swap! state/components-state assoc :main
         (merge response {:ui {:codes {:query (-> response :results :codes :query)}}})))

(def handle-code-search-requests
  (go-loop []
    (let [response (a/<! code-search-requests)]
      (if (:error response)
        (do (println "Error fetching search results!: " response)
            (swap! state/components-state assoc :main :error))
        (update-codes-state response)))
    (recur)))

(defn- navigate [route query-params]
  (rfe/push-state route {} query-params))

(defn submit-search [event]
  (.preventDefault event)
  (let [query (-> @state/components-state :main :ui :codes :query)]
    (get-code-search-results query)
    (navigate :ook.route/search {:q query})))

(defn- update-dataset-results [response]
  (swap! (r/cursor state/components-state [:main])
         (fn [old]
           (assoc-in old [:results :datasets :data] (-> response :results :datasets :data)))
         ))

(def handle-dataset-search-requests
  (go-loop []
    (let [response (a/<! dataset-search-requests)]
      (if (:error response)
        (swap! state/components-state assoc :main :error)
        (update-dataset-results response)))
    (recur)))

(defn- get-dataset-search-results [codes]
  (GET "/apply-filters"
    {:params {:code codes}
     :response-format (transit-response-format)
     :handler (fn [result] (go (a/>! dataset-search-requests result)))
     :error-handler (fn [error] (go (a/>! dataset-search-requests {:error error})))}))

(defn apply-code-selection [event]
  (.preventDefault event)
  (let [codes (->> @state/components-state :main :ui :codes :selection
                   (filter (fn [[_k v]] v))
                   (map first))
        query (-> @state/components-state :main :ui :codes :query)]
    (get-dataset-search-results codes)
    (navigate :ook.route/search {:q query :code codes})))

(defn ^:dev/before-load stop []
  (println "stopping request handlers")
  (a/close! handle-code-search-requests)
  (a/close! handle-dataset-search-requests))
