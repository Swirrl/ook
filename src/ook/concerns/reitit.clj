(ns ook.concerns.reitit
  (:require [integrant.core :as ig]
            [reitit.ring :as ring]
            [meta-merge.core :as mm]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.coercion :as coercion]))

(defmethod ig/init-key :ook.concerns.reitit/ring-handler
  [_ {:keys [router default-handler opts]}]
  (ring/ring-handler router default-handler opts))

(defmethod ig/init-key :ook.concerns.reitit/router [_ {:keys [routes opts]}]
  (ring/router routes (mm/meta-merge
                      {:middleware [;; query-params & form-params
                                    parameters/parameters-middleware
                                    ;; content-negotiation
                                    muuntaja/format-negotiate-middleware
                                    ;; encoding response body
                                    muuntaja/format-response-middleware
                                    ;; decoding request body
                                    muuntaja/format-request-middleware
                                    ;; coercing response bodys
                                    coercion/coerce-response-middleware]
                                    ;; coercing request parameters
                                    coercion/coerce-request-middleware
                                    ;; multipart
                                    multipart/multipart-middleware
                                    ;; stuff like coercion, middlewares etc. goes here
                       }
                      opts)))

(defmethod ig/init-key :ook.concerns.reitit/default-handler [_ _]
  (ring/create-default-handler
   {:not-found (constantly {:status 404, :body "404"})
    :method-not-allowed (constantly {:status 405, :body "405"})
    :not-acceptable (constantly {:status 406, :body "406"})}))

(defmethod ig/init-key :ook.concerns.reitit/resource-handler [_ opts]
  (ring/create-resource-handler opts))
