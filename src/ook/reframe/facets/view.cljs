(ns ook.reframe.facets.view
  (:require
   [re-frame.core :as rf]
   [ook.ui.icons :as icons]
   [ook.ui.common :as common]
   [ook.reframe.codes.browse.view :as browse]
   [ook.reframe.codes.search.view :as search]))

(defn- facet-button [facet-name selected? applied?]
  [:button.btn.me-2.mb-2
   {:type "button"
    :class (cond
             selected? "btn-dark"
             applied? "btn-outline-dark applied-facet"
             :else "btn-outline-dark")
    :on-click #(if applied?
                 (rf/dispatch [:ui.event/edit-facet facet-name])
                 (rf/dispatch [:ui.event/select-facet facet-name]))}
   facet-name
   (when applied?
     [:span.ms-2 {:style {:top "-1px" :position "relative"}} icons/edit])])

(defn- cancel-facet-selection []
  [:button.btn-close.border.border-dark
   {:type "button"
    :aria-label "Close filter selection"
    :on-click #(rf/dispatch [:ui.event/cancel-current-selection])}])

(defn- apply-filter-button [disabled?]
  [common/primary-button
   {:class "me-2"
    :disabled disabled?
    :on-click #(rf/dispatch [:ui.event/apply-current-facet])}
   "Apply filter"])

(defn- remove-filter-button [disabled? facet-name]
  [common/primary-button
   {:disabled disabled?
    :on-click #(rf/dispatch [:ui.event/remove-facet facet-name])}
   "Remove filter"])

(defn- facet-control-buttons [facet-name]
  (let [current-selection @(rf/subscribe [:ui.facets.current/selection])
        applied-facets @(rf/subscribe [:facets/applied])
        disabled? (empty? current-selection)]
    [:div.mt-3
     [apply-filter-button disabled?]
     (when (get applied-facets facet-name)
       [remove-filter-button disabled? facet-name])]))

(defn- codelists [facet-name]
  (when facet-name
    (if @(rf/subscribe [:ui.facets/no-codelists? facet-name])
      [:p.h6.mt-4 "No codelists for facet"]
      (let [search-status @(rf/subscribe [:ui.facets.current/search-results])]
        [:<>
         [facet-control-buttons facet-name]
         [:p.h6.mt-4 "Codelists"]
         [search/code-search]
         (if search-status
           [search/search-info facet-name]
           [browse/code-selection facet-name])]))))

(defn- codelists-wrapper [selected-facet-status facet-name]
  (when selected-facet-status
    (condp = selected-facet-status
      :loading [:div.mt-4.ms-1 [common/loading-spinner]]

      :error [common/error-message "Sorry, there was an error fetching the codelists for this facet."]

      :ready [codelists facet-name]

      [common/error-message "Sorry, something went wrong."])))

(defn configured-facets []
  (let [facets @(rf/subscribe [:facets/config])
        selected-facet-status @(rf/subscribe [:ui.facets.current/status])
        selected-facet-name @(rf/subscribe [:ui.facets.current/name])
        applied-facets @(rf/subscribe [:facets/applied])]
    [:div.filters
     [:h2 "Filters"]
     [:div.d-flex.align-items-center.justify-content-between
      [:div
       (for [{:keys [name]} facets]
         ^{:key name} [facet-button name (= name selected-facet-name) (get applied-facets name)])]
      (when (and selected-facet-name (= :ready selected-facet-status))
        [cancel-facet-selection])]
     [codelists-wrapper selected-facet-status selected-facet-name]]))
