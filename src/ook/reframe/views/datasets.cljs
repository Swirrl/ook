(ns ook.reframe.views.datasets
  (:require [re-frame.core :as rf]
            [ook.util :as u]
            [ook.params.util :as pu]))

(defn- total-observations [data]
  (reduce + (map :matching-observations data)))

(defn- remove-facet [facet-name]
  (rf/dispatch [:filters/remove-facet facet-name])
  (rf/dispatch [:app/navigate :ook.route/search]))

(defn- remove-facet-button [facet-name]
  [:button.btn-close.border.btn-sm.ms-2.align-middle
   {:type "button"
    :on-click #(remove-facet facet-name)}])

(defn- observation-count [data]
  (let [dataset-count (count data)
        obs (total-observations data)]
    (when (pos? obs)
      [:p.my-4 "Found " [:strong dataset-count (u/pluralize " dataset" dataset-count)] " covering "
       [:strong obs (u/pluralize " observation" obs)]])))

(defn- codelists-for-facet [facet-name ds-facets]
  (let [facet (->> ds-facets (filter #(= facet-name (:name %))) first)
        codelists (->> facet :dimensions (map :codelist) distinct)]
    (for [cl codelists]
      ^{:key cl} [:p.badge.bg-secondary cl])))

(defn- error-message []
  [:div.alert.alert-danger "Sorry, something went wrong."])

(defn- column-headers [data applied-facets]
  [:tr
   [:th "Title / Description"]
   (for [[facet-name _] applied-facets]
     ^{:key facet-name} [:th facet-name (remove-facet-button facet-name)])
   (when (some :matching-observations data)
     [:th])])

(defn- dataset-row [{:keys [label comment id matching-observations facets]} applied-facets]
  ^{:key id}
  [:tr
   [:td
    [:strong label]
    [:p comment]]
   (for [[facet-name _] applied-facets]
     ^{:key [id facet-name]} [:td (codelists-for-facet facet-name facets)])
   (when matching-observations
     [:td
      [:small (str "Found " matching-observations " matching observations")]
      [:div
       [:a.btn.btn-secondary.btn-sm
        {:href (pu/link-to-pmd-dataset id facets)} "View Data"]]])])

(defn- dataset-table [data applied-facets]
  (if (seq data)
    [:table.table
     [:thead (column-headers data applied-facets)]
     [:tbody (for [ds data]
               (dataset-row ds applied-facets))]]
    [:div.d-flex.align-items-center
     [:strong "No datasets matched the applied filters."]
     [:button.btn.btn-link.mx-1.p-0
      {:type "button"
       :on-click #(rf/dispatch [:init/initialize-db])}
      "Clear filters"]
     [:span "to reset and make a new selection."]]))

(defn results []
  (if @(rf/subscribe [:results.datasets/error])
    (error-message)
    (let [data @(rf/subscribe [:results.datasets/data])
          applied-facets @(rf/subscribe [:facets/applied])]
      [:<>
       (observation-count data)
       (dataset-table data applied-facets)])))
