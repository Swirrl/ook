(ns ook.reframe.views.facets
  (:require
   [re-frame.core :as rf]
   [ook.reframe.views.codes :as codes]))

(defn- facet-button [{:keys [name] :as facet} selected-facet]
  [:button.btn.me-2
   {:type "button"
    :class (if (= name (:name selected-facet)) "btn-dark" "btn-outline-dark")
    :on-click #(rf/dispatch [:ui.facets/set-current facet])}
   name])

(defn- cancel-facet-selection []
  [:button.btn-close.border.border-dark
   {:type "button"
    :aria-label "Close filter selection"
    :on-click #(rf/dispatch [:ui.facets/cancel-current-selection])}])

(defn configured-facets []
  (let [facets @(rf/subscribe [:facets/config])
        selected-facet @(rf/subscribe [:ui.facets/current])
        applied-facets @(rf/subscribe [:facets/applied])]
    [:div.card.my-4.filters
     [:div.card-body
      [:h2.h5.card-title.me-2.d-inline "Find data"]
      [:span.text-muted "Add a filter"]
      [:div.mt-3.d-flex.align-items-center.justify-content-between
       [:div
        (for [{:keys [name] :as facet} facets]
          (when-not (get applied-facets name)
            ^{:key name} [facet-button facet selected-facet]))]
       (when selected-facet
         [cancel-facet-selection])]
      [codes/codelist-selection selected-facet]]]))
