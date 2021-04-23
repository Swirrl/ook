(ns ook.reframe.views
  (:require
   [re-frame.core :as rf]
   [ook.reframe.error-boundary :as err]
   [ook.reframe.facets.view :as facets]
   [ook.reframe.datasets.view :as datasets]))

(defn search []
  [err/error-boundary
   [facets/configured-facets]
   [datasets/results]])

(defn main []
  (let [current-route @(rf/subscribe [:app/current-route])]
    (when current-route
     [(-> current-route :data :view)])))
