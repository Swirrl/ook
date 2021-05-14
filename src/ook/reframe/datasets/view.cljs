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

(defn- dataset-count-heading [data]
  (let [dataset-count (count data)
        total-dataset-count @(rf/subscribe [:datasets/count])
        obs (total-observations data)]
    (cond
      (pos? obs)
      [:h2
       dataset-count (u/pluralize " dataset" dataset-count) " covering "
       obs (u/pluralize " observation" obs)]

      (= dataset-count total-dataset-count)
      [:h2 "Datasets"]

      :else
      [:h2 dataset-count " of " total-dataset-count " datasets match"])))

(defn- matches-for-facet [facet-name ds-facets]
  (let [facet (->> ds-facets (filter #(= facet-name (:name %))) first)]
    (for [{:keys [ook/uri label codes]} (:dimensions facet)]
      ^{:key [uri label]}
      [:p
       [:span.me-1 label]
       (for [{code-uri :ook/uri code-label :label} codes]
         ^{:key code-uri}
         [:span.badge.bg-light.text-dark.rounded-pill.me-1.mt-1.text-wrap code-label])])))

(defn- error-message []
  [:div.alert.alert-danger "Sorry, something went wrong."])

(defn- facet-column-header [facet-name]
  [:th.text-nowrap
   facet-name
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
    (if label [:strong label] [:em.text-muted "Missing label for " uri])
    [:p.text-muted.me-2 (or (:altlabel publisher) "---")]
    [:p.vertical-truncate (or comment description)]]
   (for [[facet-name _] applied-facets]
     ^{:key [uri facet-name]} [:td (matches-for-facet facet-name facets)])
   (when matching-observation-count
     [:td
      [:<>
       [:p.m-0 (str "Found " matching-observation-count " matching observations")]
       [:a.d-block {:href (pu/link-to-pmd-dataset uri facets applied-facets)} "View Data"]]])])

(defn- dataset-table [data]
  (let [applied-facets @(rf/subscribe [:facets/applied])]
    [:div.ook-datasets
     [dataset-count-heading data]
     (when (seq applied-facets)
       [:p.pb-3 "For each dataset we show up to 3 examples of codes that match each facet. Empty cells indicate that the dataset doesn't match the criteria."])
     [:div
      [:table.table
       [:thead (column-headers data applied-facets)]
       [:tbody (for [ds data]
                 (dataset-row ds applied-facets))]]]]))

(defn- no-matches-message []
  [:div.ook-datasets
   [:h2 "No datasets matched the applied filters"]
   [:a.btn-link
    {:role "button"
     :on-click #(rf/dispatch [:app/navigate :ook.route/home])}
    "Clear filters"]
   [:span " to reset and make a new selection."]])

(defn results []
  (when-let [data @(rf/subscribe [:results.datasets/data])]
    (cond
      (= :loading data) [common/loading-spinner]

      (= :error data) [error-message]

      (seq data) [dataset-table data]

      :else [no-matches-message])))
