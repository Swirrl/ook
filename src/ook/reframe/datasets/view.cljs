(ns ook.reframe.datasets.view
  (:require [re-frame.core :as rf]
            [ook.util :as u]
            [ook.params.util :as pu]
            [ook.ui.common :as common]
            [ook.ui.icons :as icons]))

(defn- total-observations [data]
  (reduce + (map :matching-observation-count data)))

(defn- small-icon-button [opts icon]
  [:button.border.btn-xs.bg-white.ms-2.p-0.align-middle opts icon])

(defn- remove-facet-button [facet-name]
  [small-icon-button
   {:type "button"
    :on-click #(rf/dispatch [:ui.event/remove-facet facet-name])}
   icons/close])

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

(defn- matches-for-facet [facet-name ds-facets]
  (let [facet (->> ds-facets (filter #(= facet-name (:name %))) first)]
    (for [{:keys [ook/uri label codes]} (:dimensions facet)]
      ^{:key [uri label]}
      [:p
       [:span.me-1 label]
       (for [{code-uri :ook/uri code-label :label} codes]
         ^{:key code-uri}
         [:span.badge.bg-light.text-dark.rounded-pill.me-1 code-label])])))

(defn- error-message []
  [:div.alert.alert-danger "Sorry, something went wrong."])

(defn- edit-facet-button [facet-name]
  [small-icon-button
   {:type "button"
    :on-click #(rf/dispatch [:ui.event/remove-facet facet-name])}
   icons/edit])

(defn- facet-column-header [facet-name]
  [:th.text-nowrap
   facet-name
   (edit-facet-button facet-name)
   [remove-facet-button facet-name]])

(defn- column-headers [data applied-facets]
  [:tr
   [:th.title-column "Publisher / Title / Description"]
   (for [[facet-name _] applied-facets]
     ^{:key facet-name}[facet-column-header facet-name])
   (when (some :matching-observation-count data)
     [:th])])

(defn- dataset-row [{:keys [label publisher comment description ook/uri matching-observation-count facets]}
                    applied-facets]
  ^{:key uri}
  [:tr
   [:td.title-column
    [:span.text-muted.me-2 (or (:altlabel publisher) "---")]
    (if label [:strong label] [:em.text-muted "Missing label for " uri])
    [:small.vertical-truncate (or comment description)]]
   (for [[facet-name _] applied-facets]
     ^{:key [uri facet-name]} [:td (matches-for-facet facet-name facets)])
   [:td
    (when matching-observation-count
      [:<>
       [:small (str "Found " matching-observation-count " matching observations")]
       [:a.d-block {:href (pu/link-to-pmd-dataset uri facets)} "View Data"]])]])

(defn- dataset-table [data]
  (let [applied-facets @(rf/subscribe [:facets/applied])]
    [:<>
     (dataset-count-message data)
     (when (seq applied-facets)
       [:p "For each dataset we show one example of a code that matches each facet. Empty cells indicate that the dataset doesn't match the criteria."])
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
     :on-click #(rf/dispatch [:ui.event/clear-filters])}
    "Clear filters"]
   [:span " to reset and make a new selection."]])

(defn results []
  (when-let [data @(rf/subscribe [:results.datasets/data])]
    (cond
      (= :loading data) [common/loading-spinner]

      (= :error data) [error-message]

      (seq data) [dataset-table data]

      :else [no-matches-message])))
