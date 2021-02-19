(ns ook.handler
  (:require [integrant.core :as ig]
            [ring.util.response :as resp]
            [ook.ui.layout :as layout]
            [ook.search.db :as db]
            [ook.params.parse :as p]
            [ook.concerns.transit :as t]))

(defmethod ig/init-key :ook.handler/main [_ _]
  (fn [_request]
    (resp/response (layout/->html (layout/search)))))

(defn- requesting-transit? [{:keys [headers]}]
  (let [accept (headers "accept")]
    (= "application/transit+json" accept)))

(defn- response [request result]
  (if (requesting-transit? request)
    (-> (resp/response (t/write-string result))
        (resp/header "Content-Type" "application/transit+json"))
    (resp/response (layout/->html (layout/search result)))))

(defmethod ig/init-key :ook.handler/search [_ {:keys [search/db]}]
  (fn [request]
    (let [query (or (p/get-query request) "")
          result (db/get-codes db query)]
      (response request result))))

(defmethod ig/init-key :ook.handler/apply-filters [_ {:keys [search/db]}]
  (fn [request]
    (let [filters (p/parse-filters request)
          result (db/get-datasets db filters)]
      (response request result))))
