(ns ook.reframe.views
  (:require
   [re-frame.core :as rf]))

(defn main []
  (let [current-route @(rf/subscribe [:app/current-route])]
    (when current-route
     [(-> current-route :data :view)])))
