(ns ook.handler
  (:require [integrant.core :as ig]
            [ring.util.response :as resp]
            [ook.ui.layout :as layout]
            [ook.search.db :as db]
            [ook.params.parse :as p]
            [ook.concerns.transit :as t]))

;; App entry handler

(defmethod ig/init-key :ook.handler/main [_ {:keys [search/db]}]
  (fn [_request]
    (let [facets (db/get-facets db)]
      (resp/response (layout/->html (layout/main {:facets facets
                                                  :dataset-count (db/dataset-count db)}))))))

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
      ;; Old implementation that applied a custom code selection.
      ;; When this comes back, combine it with other filter facets
      ;; (let [filters (p/parse-filters request)
      ;;       result (db/get-datasets db filters)]
      ;;   (-> (resp/response (t/write-string result))
      ;;       transit-content-type))
      (let [facets (p/get-facets request)
            datasets (if facets
                       (db/get-datasets-for-facets db facets)
                       (db/all-datasets db))]
        (transit-response datasets))
      invalid-format-response)))

(defmethod ig/init-key :ook.handler/codes [_ {:keys [search/db]}]
  (fn [request]
    (if (requesting-transit? request)
      (->> request
           p/get-dimensions
           (db/get-code-trees db)
           transit-response)
      invalid-format-response)))
