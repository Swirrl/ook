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

(defmethod ig/init-key :ook.concerns.reitit/router [_ {:keys [data opts]}]
  (ring/router data (mm/meta-merge
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
