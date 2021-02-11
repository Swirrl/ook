(ns ook.handler
  (:require [integrant.core :as ig]
            [ring.util.response :as resp]
            [ook.ui.layout :as layout]
            [ook.search.elastic :as search]
            [ook.ui.results :as results]
            [ook.params.parse :as p]))

(defn- home []
  [:div {:id ":home" :class "OokComponent" :data-ook-init "initial-state" }
   [:p "Loading..."]])

(defmethod ig/init-key :ook.handler/home [_ _]
  (fn [_request]
    (resp/response (layout/->html (home)))))

(defmethod ig/init-key :ook.handler/search [_ {:keys [es-endpoint]}]
  (fn [request]
    (let [query (p/get-query request)
          result (search/query es-endpoint query)]
      (resp/response (layout/->html (results/search-results result))))))
