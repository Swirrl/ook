(ns ook.reframe.views
  (:require
   [re-frame.core :as rf]
   [ook.reframe.views.facets :as facets]
   [ook.reframe.views.datasets :as datasets]))

(defn search []
  [:<>
   (facets/configured-facets)
   (datasets/results)])

(defn main []
  (let [current-route @(rf/subscribe [:app/current-route])]
    (when current-route
     [(-> current-route :data :view)])))
