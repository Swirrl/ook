(ns ook.handler
  (:require [ook.ui.state :as state]
            [ajax.core :refer [GET transit-response-format]]))

;; (defn set-query-string! [query-string]
;;   (let [current-path (h/current-url-path)
;;         new-url (cond-> current-path
;;                   (seq query-string) (str "?" query-string))]
;;     (-> js/window .-history (.replaceState nil "" new-url))))

;; (defn update-url-query-params [state]
;;   (set-query-string! (state->query-string state)))

(defn- get-search-results [query]
  (GET "/search"
    {:params {:q query}
     :response-format (transit-response-format)
     :handler  (fn [result]
                    ;; update url
                 (swap! state/components-state assoc :search result))
     :error-handler (fn [error]
                      (println "Error fetching search results!: " error)
                      (swap! state/components-state assoc :search :error))}))

(defn submit-search [event]
  (.preventDefault event)
  (let [query (-> event .-target js/FormData. (.get "q"))]
    (get-search-results query)))
