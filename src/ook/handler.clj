(ns ook.handler
  (:require [integrant.core :as ig]
            [ring.util.response :as resp]
            [ook.ui.layout :as layout]
            [ook.search.db :as db]
            [ook.params.parse :as p]
            [ook.concerns.transit :as t]))

;; App entry handler

(defmethod ig/init-key :ook.handler/main [_ _]
  (fn [_request]
    (resp/response (layout/->html (layout/main)))))

;;; Internal transit API

(defn- requesting-transit? [{:keys [headers]}]
  (let [accept (headers "accept")]
    (= "application/transit+json" accept)))

(def invalid-format-response
  {:status  406 :headers {} :body "Unsupported content type"})

(defn- transit-content-type [response]
  (-> response (resp/header "Content-Type" "application/transit+json")))

(defmethod ig/init-key :ook.handler/get-codes [_ {:keys [search/db]}]
  (fn [request]
    (if (requesting-transit? request)
      (let [query (or (p/get-query request) "")
            codes (db/get-codes db query)]
        (-> (resp/response (t/write-string codes))
            transit-content-type))
      invalid-format-response)))

(defmethod ig/init-key :ook.handler/apply-filters [_ {:keys [search/db]}]
  (fn [request]
    (if (requesting-transit? request)
      (let [filters (p/parse-filters request)
            result (db/get-datasets db filters)]
        (-> (resp/response (t/write-string result))
            transit-content-type))
      invalid-format-response)))

(defmethod ig/init-key :ook.handler/datasets [_ {:keys [search/db]}]
  (fn [request]
    (if (requesting-transit? request)
      (-> (db/all-datasets db)
          t/write-string
          resp/response
          transit-content-type)
      invalid-format-response)))
