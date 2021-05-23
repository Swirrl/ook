(ns ook.reframe.facets.view
  (:require
   [re-frame.core :as rf]
   [ook.ui.icons :as icons]
   [ook.ui.common :as common]
   [ook.reframe.codes.browse.view :as browse]
   [ook.reframe.codes.search.view :as search]))

(defn- facet-button [facet-name selected? applied?]
  [:button.btn.me-2
   {:type "button"
    :class (cond
             selected? "btn-dark"
             applied? "btn-outline-dark applied-facet"
             :else "btn-outline-dark")
    :aria-label (cond
                  selected? (str "Close " facet-name " filter")
                  applied? (str "Edit " facet-name " filter")
                  :else (str "Open " facet-name " filter"))
    :on-click #(cond
                 selected? (rf/dispatch [:ui.event/cancel-current-selection])
                 applied? (rf/dispatch [:ui.event/edit-facet facet-name])
                 :else (rf/dispatch [:ui.event/select-facet facet-name]))}
   facet-name
   (when applied?
     [:span.ms-2.edit-facet
      {:role "button" :style {:top "-1px" :position "relative"}} icons/edit])
   (when selected?
     [:span.ms-2.close-facet {:role "button"} icons/close])])

(defn- apply-filter-button [disabled?]
  [common/primary-button
   {:class "me-2"
    :disabled disabled?
    :on-click #(rf/dispatch [:ui.event/apply-current-facet])}
   "Apply filter"])

(defn- remove-filter-button [facet-name]
  [common/primary-button
   {:on-click #(rf/dispatch [:ui.event/remove-facet facet-name])}
   "Remove filter"])

(defn- facet-control-buttons [facet-name]
  (let [current-selection @(rf/subscribe [:ui.facets.current/selection])
        applied-facets @(rf/subscribe [:facets/applied])
        disabled? (empty? current-selection)]
    [:div.mt-3
     [apply-filter-button disabled?]
     (when (get applied-facets facet-name)
       [remove-filter-button facet-name])]))

(defn- codelists [facet-name]
  (when facet-name
    (if @(rf/subscribe [:ui.facets/no-codelists? facet-name])
      [:p.h6.mt-4 "No codelists for facet"]
      (let [search-status @(rf/subscribe [:ui.facets.current/search-status])]
        [:<>
         [facet-control-buttons facet-name]
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
    [:div.pb-3.my-5.filters
     [:h2.pb-2 "Filters"]
     [:div.d-flex.align-items-center
      [:div
       (for [{:keys [name]} facets]
         ^{:key name} [facet-button name (= name selected-facet-name) (get applied-facets name)])]]
     [codelists-wrapper selected-facet-status selected-facet-name]]))
