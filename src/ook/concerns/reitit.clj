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

(defmethod ig/init-key :ook.concerns.reitit/default-handler [_ {:keys [handler/exception-handler]}]
  (ring/routes
   (ring/redirect-trailing-slash-handler)
   (ring/create-default-handler
    {:not-found (partial exception-handler 404 "Page not found." nil)
     :method-not-allowed (partial exception-handler 405 "Method not allowed." nil)
     :not-acceptable (partial exception-handler 406 "Request not acceptable." nil)})))

(defmethod ig/init-key :ook.concerns.reitit/error-handler [_ {:keys [assets/fingerprinter]}]
  (fn [status message exception request]
    {:status status
     :body (layout/->html (layout/error-page status message fingerprinter))}))

(defmethod ig/init-key :ook.concerns.reitit/exception-middleware [_ {:keys [handler/exception-handler]}]
  ;; custom-configured reitit exception middleware
  ;; https://github.com/metosin/reitit/blob/master/doc/ring/exceptions.md
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {java.net.ConnectException (partial exception-handler 500 "Error connecting to elasticsearch.")

     ;; ex-data with :type ::error
     ::exception/error (partial exception-handler "error")

     ;; ex-data with ::exception or ::failure
     ::exception/exception (partial exception-handler "exception")

     ::exception/default (partial exception-handler 500 "Error.")

       ;; print stack-traces for all exceptions
     ::exception/wrap (fn [handler error request]
                        (log/error "Error fetching uri: " (:uri request))
                        (log/error
                         (if-let [data (-> error Throwable->map)]
                           (str (:cause data)
                                (if-let [body (get-in data [:data :body])]
                                  body))
                           (if-let [trace (.getStackTrace error)]
                             trace)))
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
