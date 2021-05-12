(ns ook.concerns.reitit
  (:require [integrant.core :as ig]
            [reitit.ring :as ring]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.coercion :as coercion]))

(def ^:private middleware [parameters/parameters-middleware ;; query-params & form-params
                           muuntaja/format-negotiate-middleware ;; content-negotiation
                           muuntaja/format-response-middleware ;; encoding response body
                           exception/exception-middleware ;; exception handling
                           muuntaja/format-request-middleware ;; decoding request body
                           coercion/coerce-response-middleware ;; coercing response bodys
                           coercion/coerce-request-middleware ;; coercing request parameters
                           multipart/multipart-middleware ;; multipart
                           ])

(defmethod ig/init-key :ook.concerns.reitit/ring-handler
  [_ {:keys [router default-handler]}]
  (ring/ring-handler router default-handler {:middleware middleware}))

(defmethod ig/init-key :ook.concerns.reitit/router [_ {:keys [routes]}]
  (ring/router routes))

(defmethod ig/init-key :ook.concerns.reitit/default-handler [_ _]
  (ring/create-default-handler
   {:not-found (constantly {:status 404, :body "404"})
    :method-not-allowed (constantly {:status 405, :body "405"})
    :not-acceptable (constantly {:status 406, :body "406"})}))
