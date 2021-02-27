(ns ook.handler
  (:require [integrant.core :as ig]
            [ring.util.response :as resp]
            [ook.ui.layout :as layout]
            [ook.search.db :as db]
            [ook.params.parse :as p]
            [ook.concerns.transit :as t]))

;; HTML handlers to support permalink copy/pasting and no-JS

;; (defmethod ig/init-key :ook.handler/main [_ _]
;;   (fn [_request]
;;     (resp/response (layout/->html (layout/main)))))

(defn- requesting-transit? [{:keys [headers]}]
  (let [accept (headers "accept")]
    (= "application/transit+json" accept)))



;; Transit-only handlers for fetching partial UI data when JS is available



;;;;;;;;;;;;;;; NEW

(defmethod ig/init-key :ook.handler/main [_ _]
  (fn [_request]
    (resp/response (layout/->html (layout/main)))))

;;; API

(defmethod ig/init-key :ook.handler/get-codes [_ {:keys [search/db]}]
  (fn [request]
    (let [query (or (p/get-query request) "")
          ;; filters (p/parse-filters request)
          codes (db/get-codes db query)
          ;; datasets (when filters (db/get-datasets db filters))
          ]
      (if (requesting-transit? request)
        (-> (resp/response (t/write-string codes))
            (resp/header "Content-Type" "application/transit+json"))
        {:status  406 :headers {} :body "Unsupported content type"}))))

(defmethod ig/init-key :ook.handler/apply-filters [_ {:keys [search/db]}]
  (fn [request]
    (let [filters (p/parse-filters request)
          result (db/get-datasets db filters)]
      (if (requesting-transit? request)
        (-> (resp/response (t/write-string result))
            (resp/header "Content-Type" "application/transit+json"))
        {:status  406 :headers {} :body "Unsupported content type"}))))
