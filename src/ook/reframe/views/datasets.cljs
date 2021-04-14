(ns ook.reframe.views.datasets
  (:require [re-frame.core :as rf]
            [ook.util :as u]
            [ook.params.util :as pu]))

(defn- total-observations [data]
  (reduce + (map :matching-observation-count data)))

(defn- remove-facet-button [facet-name]
  [:button.btn-close.border.btn-xs.ms-2.align-middle
   {:type "button"
    :on-click #(rf/dispatch [:filters/remove-facet facet-name])}])

(defn- dataset-count-message [data]
  (let [dataset-count (count data)
        total-dataset-count @(rf/subscribe [:datasets/count])
        obs (total-observations data)]
    (cond
      (pos? obs)
      [:p.my-4 "Found " [:strong dataset-count (u/pluralize " dataset" dataset-count)] " covering "
       [:strong obs (u/pluralize " observation" obs)]]

      (= dataset-count total-dataset-count)
      [:p.my-4 "Showing all datasets"]

      (empty? data)
      [:div.d-flex.align-items-center
       [:strong "No datasets matched the applied filters. "]
       [:a.btn-link.mx-1
        {:role "button"
         :on-click #(rf/dispatch [:filters/reset])}
        "Clear filters"]
       [:span " to reset and make a new selection."]]

      :else
      [:p.my-4
       [:strong dataset-count] " of "
       [:strong total-dataset-count]
       " datasets match"])))

(defn- codelists-for-facet [facet-name ds-facets]
  (let [facet (->> ds-facets (filter #(= facet-name (:name %))) first)
        codelists (->> facet :dimensions (mapcat :codelists) distinct)]
    (for [{:keys [ook/uri label]} codelists]
      ^{:key uri} [:p.badge.bg-secondary.me-1 label])))

(defn- error-message []
  [:div.alert.alert-danger "Sorry, something went wrong."])

(defn- column-headers [data applied-facets]
  [:tr
   [:th.title-column "Title / Description"]
   (for [[facet-name _] applied-facets]
     ^{:key facet-name} [:th.text-nowrap facet-name (remove-facet-button facet-name)])
   (when (some :matching-observation-count data)
     [:th])])

(defn- dataset-row [{:keys [label comment ook/uri matching-observation-count facets]} applied-facets]
  ^{:key uri}
  [:tr
   [:td.title-column
    [:strong label]
    [:p.vertical-truncate comment]]
   (for [[facet-name _] applied-facets]
     ^{:key [uri facet-name]} [:td (codelists-for-facet facet-name facets)])
   (when matching-observation-count
     [:td
      [:small (str "Found " matching-observation-count " matching observations")]
      [:div
       [:a.btn.btn-secondary.btn-sm
        {:href (pu/link-to-pmd-dataset uri facets)} "View Data"]]])])

(defn- dataset-table [data applied-facets]
  (when (seq data)
    [:div.ook-datasets
     [:table.table
      [:thead (column-headers data applied-facets)]
      [:tbody (for [ds data]
                (dataset-row ds applied-facets))]]]))

(defn results []
  (if @(rf/subscribe [:results.datasets/error])
    [error-message]
    (let [data @(rf/subscribe [:results.datasets/data])
          applied-facets @(rf/subscribe [:facets/applied])]
      [:<>
       (dataset-count-message data)
       (dataset-table data applied-facets)])))
