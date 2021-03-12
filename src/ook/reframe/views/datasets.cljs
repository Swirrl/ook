(ns ook.reframe.views.datasets
  (:require [re-frame.core :as rf]
            [ook.util :as u]))

(defn- total-observations [data]
  (reduce + (map :matching-observations data)))

(defn results []
  (let [data @(rf/subscribe [:results.datasets/data])
        dataset-count (count data)
        obs (total-observations data)]
    (when data
      [:<>
       (when (pos? obs)
         [:p.my-4 "Found " [:strong dataset-count (u/pluralize " dataset" dataset-count)] " covering "
          [:strong obs (u/pluralize " observation" obs)]])
       (when (seq data)
         [:table.table.my-4
          [:thead
           [:tr
            [:th {:scope "col"} "Title / Description"]
            [:th {:scope "col"}]]]
          [:tbody
           (for [{:keys [label comment id matching-observations]} data]
             ^{:key id}
             [:tr
              [:td
               [:strong label]
               [:p comment]]
              (when matching-observations
                [:td
                 [:small (str "Found " matching-observations " matching observations")]
                 [:div
                  [:a.btn.btn-secondary.btn-sm {:href "#"} "View Data"]]])])]])])))
