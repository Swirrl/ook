(ns ook.reframe.views
  (:require
   [re-frame.core :as rf]
   [ook.reframe.views.search :as search]
   [ook.reframe.views.filters :as filters]
   [ook.reframe.views.datasets :as datasets]))

(defn home []
  (let [facets @(rf/subscribe [:facets/config])]
    [:<>
     (filters/configured-facets facets)
     (datasets/results)]))

(defn results []
  [:<>
   ;; (search/create-filter-card
   ;;   (filters/filters))
   (datasets/results)])

(defn main []
  (let [current-route @(rf/subscribe [:app/current-route])]
    (when current-route
     [(-> current-route :data :view)])))
