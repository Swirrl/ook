(ns ook.handler
  (:require [integrant.core :as ig]))

(defmethod ig/init-key :ook.handler/default-handler [_ opts]
  (fn [request]
    {:body "Hello world"}))
