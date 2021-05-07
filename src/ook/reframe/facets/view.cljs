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

(defn- apply-filter-button []
  (let [current-selection @(rf/subscribe [:ui.facets.current/selection])]
    [common/primary-button
     {:class "mt-3"
      :disabled (empty? current-selection)
      :on-click #(rf/dispatch [:ui.event/apply-current-facet])}
     "Apply filter"]))

(defn- codelists [name]
  (when name
    (let [codelists @(rf/subscribe [:ui.facets.current/visible-codes name])
          search-status @(rf/subscribe [:ui.facets.current/search-results])]
      (if (= :no-codelists codelists)
        [:p.h6.mt-4 "No codelists for facet"]
        [:<>
         [apply-filter-button]
         [:p.h6.mt-4 "Codelists"]
         [search/code-search]
         (if search-status
           [search/search-info codelists]
           [browse/code-selection codelists])]))))

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
    [:div.card.my-4.filters
     [:div.card-body
      [:h2.h5.card-title.me-2.d-inline "Find data"]
      [:span.text-muted "Add a filter"]
      [:div.mt-3.d-flex.align-items-center.justify-content-between
       [:div
        (for [{:keys [name]} facets]
          ^{:key name} [facet-button name (= name selected-facet-name) (get applied-facets name)])]
       (when (and selected-facet-name (= :ready selected-facet-status))
         [cancel-facet-selection])]
      [codelists-wrapper selected-facet-status selected-facet-name]]]))
