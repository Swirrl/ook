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
  [:button.btn-close.border.btn-xs.ms-2.align-middle
   {:type "button"
    :on-click #(remove-facet facet-name)}])

(defn- dataset-count [data]
  (let [dataset-count (count data)
        total-dataset-count @(rf/subscribe [:datasets/count])
        obs (total-observations data)]
    (cond
      (pos? obs)
      [:p.my-4 "Found " [:strong dataset-count (u/pluralize " dataset" dataset-count)] " covering "
       [:strong obs (u/pluralize " observation" obs)]]

      (= dataset-count total-dataset-count)
      [:p.my-4 "Showing all datasets"]

      :else
      [:p.my-4
       [:strong dataset-count] " of "
       [:strong total-dataset-count]
       " datasets match"])))

(defn- codelists-for-facet [facet-name ds-facets]
  (let [facet (->> ds-facets (filter #(= facet-name (:name %))) first)
        codelists (->> facet :dimensions (map :codelist) distinct)]
    (for [{:keys [id label]} codelists]
      ^{:key id} [:p.badge.bg-secondary.me-1 label])))

(defn- error-message []
  [:div.alert.alert-danger "Sorry, something went wrong."])

(defn- column-headers [data applied-facets]
  [:tr
   [:th.title-column-header "Title / Description"]
   (for [[facet-name _] applied-facets]
     ^{:key facet-name} [:th.text-nowrap facet-name (remove-facet-button facet-name)])
   (when (some :matching-observations data)
     [:th])])

(defn- dataset-row [{:keys [label comment id matching-observations facets]} applied-facets]
  ^{:key id}
  [:tr
   [:td
    [:strong label]
    [:p.vertical-truncate comment]]
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
    [:div.ook-datasets
     [:table.table
      [:thead (column-headers data applied-facets)]
      [:tbody (for [ds data]
                (dataset-row ds applied-facets))]]]
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
       (dataset-count data)
       (dataset-table data applied-facets)])))
