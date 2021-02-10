(ns ook.handler
  (:require [integrant.core :as ig]
            [ring.util.response :as resp]
            [ook.ui.layout :as layout]
            [ook.search.elastic :as search]
            [ook.ui.results :as results]))

(defn- home []
  [:div {:id ":home" :class "OokComponent" :data-ook-init "initial-state" }
   [:p "Loading..."]])

(defmethod ig/init-key :ook.handler/home [_ _]
  (fn [_request]
    (resp/response (layout/->html (home)))))

(defmethod ig/init-key :ook.handler/search [_ _]
  (fn [request]
    (let [result (search/query "car")]
      (resp/response (layout/->html (results/search-results result))))))
