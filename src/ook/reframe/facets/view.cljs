(ns ook.reframe.facets.view
  (:require
   [re-frame.core :as rf]
   [ook.reframe.codes.view :as codes]
   [ook.ui.icons :as icons]))

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
      [codes/codelist-selection selected-facet-status selected-facet-name]]]))
