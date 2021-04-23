(ns ook.reframe.facets.view
  (:require
   [re-frame.core :as rf]
   [ook.reframe.codes.view :as codes]))

(defn- facet-button [{:keys [name] :as facet} selected-facet-name]
  [:button.btn.me-2
   {:type "button"
    :class (if (= name selected-facet-name) "btn-dark" "btn-outline-dark")
    :on-click #(rf/dispatch [:ui.event/set-current facet])}
   name])

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
        (for [{:keys [name] :as facet} facets]
          (when-not (get applied-facets name)
            ^{:key name} [facet-button facet selected-facet-name]))]
       (when (= :success/ready selected-facet-status)
         [cancel-facet-selection])]
      [codes/codelist-selection selected-facet-status]]]))
