(ns ook.concerns.jetty
  (:require [ring.adapter.jetty :as jetty]
            [integrant.core :as ig]))

(defmethod ig/init-key :ook.concerns/jetty [_ {:keys [handler opts]}]
  (jetty/run-jetty handler opts))

(defmethod ig/halt-key! :ook.concerns/jetty [_ jetty]
  (.stop jetty))
