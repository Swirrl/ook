(ns ook.reframe.views.datasets
  (:require [re-frame.core :as rf]
            [ook.util :as u]
            [ook.params.util :as pu]))

(defn- total-observations [data]
  (reduce + (map :matching-observations data)))

(defn- remove-facet-button [facet-name]
  [:button {:type "button"} "x"])

(defn- observation-count [data]
  (let [dataset-count (count data)
        obs (total-observations data)]
    (when (pos? obs)
      [:p.my-4 "Found " [:strong dataset-count (u/pluralize " dataset" dataset-count)] " covering "
       [:strong obs (u/pluralize " observation" obs)]])))

(defn- codelists-for-facet [facet-name ds-facets]
  (let [facet (->> ds-facets (filter #(= facet-name (:name %))) first)
        codelists (->> facet :dimensions (map :codelist))]
    (for [cl codelists]
      ^{:key cl}[:p.badge.bg-secondary cl])))

(defn results []
  (let [data @(rf/subscribe [:results.datasets/data])]
    (if @(rf/subscribe [:results.datasets/error])
      [:div.alert.alert-danger "Sorry, something went wrong."]
      (let [applied-facets @(rf/subscribe [:facets/applied])]
        [:<>
         (observation-count data)
         (when (seq data)
           [:table.table
            [:thead
             [:tr
              [:th "Title / Description"]
              (for [[facet-name _] applied-facets]
                ^{:key facet-name}[:th facet-name (remove-facet-button facet-name)])
              (when (some :matching-observations data)
                [:th])]]
            [:tbody
             (for [{:keys [label comment id matching-observations facets] :as ds} data]
               ^{:key id}
               [:tr
                [:td
                 [:strong label]
                 [:p comment]]
                (for [[facet-name _] applied-facets]
                  ^{:key [id facet-name]}[:td (codelists-for-facet facet-name facets)])
                (when matching-observations
                  [:td
                   [:small (str "Found " matching-observations " matching observations")]
                   [:div
                    [:a.btn.btn-secondary.btn-sm
                     {:href (pu/link-to-pmd-dataset id facets)} "View Data"]]])])]])]))))
