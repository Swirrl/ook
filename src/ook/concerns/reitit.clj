(ns ook.concerns.reitit
  (:require [integrant.core :as ig]
            [reitit.ring :as ring]
            [ring.util.response :as resp]
            [clojure.tools.logging :as log]
            [ook.ui.layout :as layout]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.coercion :as coercion]))

(defmethod ig/init-key :ook.concerns.reitit/ring-handler
  [_ {:keys [router default-handler middleware]}]
  (ring/ring-handler router default-handler {:middleware middleware}))

(defmethod ig/init-key :ook.concerns.reitit/router [_ {:keys [routes]}]
  (ring/router routes))

(defmethod ig/init-key :ook.concerns.reitit/default-handler [_ _]
  (ring/routes
   (ring/redirect-trailing-slash-handler)
   (ring/create-default-handler
    {:not-found (constantly {:status 404, :body "404" :headers {}})
     :method-not-allowed (constantly {:status 405, :body "405" :headers {}})
     :not-acceptable (constantly {:status 406, :body "406" :headers {}})})))

(defmethod ig/init-key :ook.concerns.reitit/error-handler [_ {:keys [assets/fingerprinter]}]
  (fn [message exception request]
    {:status 500
     :body (layout/->html (layout/error-page "500" message fingerprinter))}))

(defmethod ig/init-key :ook.concerns.reitit/exception-middleware [_ {:keys [handler/exception-handler]}]
  ;; custom-configured reitit exception middleware
  ;; https://github.com/metosin/reitit/blob/master/doc/ring/exceptions.md
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {java.net.ConnectException (partial exception-handler "Error connecting to elasticsearch.")

     ::exception/default (partial exception-handler "Error.")

       ;; print stack-traces for all exceptions
     ::exception/wrap (fn [handler error request]
                        (log/error "Error fetching uri: " (:uri request))
                        (log/info "500:" (-> error Throwable->map :cause))
                        (handler error request))})))

(defmethod ig/init-key :ook.concerns.reitit/middleware [_ {:keys [middleware/exceptions]}]
  [parameters/parameters-middleware ;; query-params & form-params
   muuntaja/format-negotiate-middleware ;; content-negotiation
   muuntaja/format-response-middleware ;; encoding response body
   exceptions ;; exception handling
   muuntaja/format-request-middleware ;; decoding request body
   coercion/coerce-response-middleware ;; coercing response bodys
   coercion/coerce-request-middleware ;; coercing request parameters
   multipart/multipart-middleware ;; multipart
   ])
