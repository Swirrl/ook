(ns ook.reframe.views.datasets
  (:require [re-frame.core :as rf]
            [ook.util :as u]
            [ook.params.util :as pu]
            [ook.ui.common :as common]))

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

(defn- dataset-table [data]
  (let [applied-facets @(rf/subscribe [:facets/applied])]
    [:<>
     (dataset-count-message data)
     [:div.ook-datasets
      [:table.table
       [:thead (column-headers data applied-facets)]
       [:tbody (for [ds data]
                 (dataset-row ds applied-facets))]]]]))

(defn- no-matches-message []
  [:div.d-flex.align-items-center
   [:strong "No datasets matched the applied filters. "]
   [:a.btn-link.mx-1
    {:role "button"
     :on-click #(rf/dispatch [:filters/reset])}
    "Clear filters"]
   [:span " to reset and make a new selection."]])

(defn results []
  (when-let [data @(rf/subscribe [:results.datasets/data])]
    (cond
      (= :loading data) [common/loading-spinner]

      (= :error data) [error-message]

      (seq data) [dataset-table data]

      :else [no-matches-message])))
