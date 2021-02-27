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
       [:p.my-4 "Found " [:strong dataset-count (u/pluralize " dataset" dataset-count)] " covering "
        [:strong obs (u/pluralize " observation" obs)]]
       (when (seq data)
         [:table.table
          [:thead
           [:tr
            [:th {:scope "col"} "Dataset Id"]
            [:th {:scopt "col"}]]]
          [:tbody
           (for [{:keys [dataset matching-observations]} data]
             ^{:key dataset}
             [:tr
              [:th {:scope "row"} dataset]
              [:td
               [:small (str "Found " matching-observations " matching observations")]
               [:div
                [:a.btn.btn-secondary.btn-sm {:href "#"} "View Data"]]]])]])])))
