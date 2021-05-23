(ns ook.handler
  (:require [integrant.core :as ig]
            [ring.util.response :as resp]
            [ook.ui.layout :as layout]
            [ook.search.db :as db]
            [ook.concerns.asset-fingerprinting :as assets]
            [ook.params.parse :as p]
            [ook.concerns.transit :as t]
            [ook.util :as u]))

;; App entry handler

(defmethod ig/init-key :ook.handler/main [_ {:keys [assets/fingerprinter search/db]}]
  (fn [_request]
    (let [facets (db/get-facets db)]
      (resp/response (layout/->html (layout/main
                                     fingerprinter
                                     {:facets (u/lookup :name facets)
                                      :dataset-count (db/dataset-count db)}))))))

;; Static resource handler

(defmethod ig/init-key :ook.handler/assets [_ opts]
  (partial assets/resource-handler opts))

;;; Internal transit API

(defn- requesting-transit? [{:keys [headers]}]
  (let [accept (headers "accept")]
    (= "application/transit+json" accept)))

(def invalid-format-response
  {:status 406 :headers {} :body "Unsupported content type"})

(defn- transit-content-type [response]
  (-> response (resp/header "Content-Type" "application/transit+json")))

(defn- transit-response [body]
  (-> body t/write-string resp/response transit-content-type))

(defmethod ig/init-key :ook.handler/datasets [_ {:keys [search/db]}]
  (fn [request]
    (if (requesting-transit? request)
      (let [facets (p/get-facets request)
            datasets (if facets
                       (db/get-datasets-for-facets db facets)
                       (db/all-datasets db))]
        (transit-response datasets))
      invalid-format-response)))

(defmethod ig/init-key :ook.handler/codelists [_ {:keys [search/db]}]
  (fn [request]
    (if (requesting-transit? request)
      (->> request p/get-dimensions (db/components->codelists db) transit-response)
      invalid-format-response)))

(defmethod ig/init-key :ook.handler/codes [_ {:keys [search/db]}]
  (fn [request]
    (if (requesting-transit? request)
      (->> request p/get-codelist (db/get-concept-tree db) transit-response)
      invalid-format-response)))

(defmethod ig/init-key :ook.handler/code-search [_ {:keys [search/db]}]
  (fn [request]
    (if (requesting-transit? request)
      (->> request p/get-search-params (db/search-codes db) transit-response)
      invalid-format-response)))
