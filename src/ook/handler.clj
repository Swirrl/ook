(ns ook.handler
  (:require [integrant.core :as ig]
            [ring.util.response :as resp]
            [ook.ui.layout :as layout]
            [cljs.reader :as edn]))

(defn- home []
  [:div {:id ":home" :class "OokComponent" :data-ook-init "initial-state" }
   [:h1 "Hello Swirrld"]])

(defmethod ig/init-key :ook.handler/home [_ _]
  (fn [_request]
    (resp/response (layout/->html (home)))))
