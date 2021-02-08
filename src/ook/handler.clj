(ns ook.handler
  (:require [integrant.core :as ig]
            [ring.util.response :as resp]
            [ook.ui.layout :as layout]))

(defn- home []
  [:h1 "Hello Swirrl"])

(defmethod ig/init-key :ook.handler/default-handler [_ opts]
  (fn [request]
    (resp/response (layout/->html (home)))))
