(ns ook.reframe.views.filters
  (:require
   [re-frame.core :as rf]
   [ook.ui.icons :as icons]))

(defn- apply-filter-button [{:keys [codelists selection]}]
  [:button.btn.btn-primary.mt-3
   {:type "button"
    :disabled (or (not (seq codelists)) (not (seq selection)))
    :on-click #(rf/dispatch [:filters/add-current-facet])}
   "Apply filter"])

(defn- text-button [opts & children]
  [:button.btn.btn-link.mx-1.p-0.align-baseline
   (merge opts {:type "button"})
   children])

(defn- toggle-level-button [opts expanded?]
  [:button.btn.as-link.p-0.m-0.expand-code-button
   (merge opts {:type "button"})
   (if expanded? icons/down icons/up)])

(defn- toggle-code-expanded-button [{:keys [ook/uri]} expanded?]
  [toggle-level-button
   {:on-click #(rf/dispatch [:ui.facets.current/toggle-expanded uri])}
   expanded?])

(defn- toggle-codelist-expanded-button [codelist expanded?]
  [toggle-level-button
   {:on-click #(rf/dispatch [:ui.facets.current/toggle-codelist codelist])}
   expanded?])

(defn- select-any-button [{:keys [ook/uri]}]
  [text-button
   {:on-click #(rf/dispatch [:ui.facets.current/set-selection :any uri])}
   "any"])

(defn- select-all-children-button [{:keys [ook/uri]}]
  [text-button
   {:on-click #(rf/dispatch [:ui.facets.current/set-selection :children uri])}
   "all children"])

(defn- checkbox-input [{:keys [ook/uri disabled?] :as option}]
  (let [selected? @(rf/subscribe [:ui.facets.current/option-selected? option])]
    [:input.form-check-input.mx-2
     (cond-> {:type "checkbox"
              :name "code"
              :value uri
              :id uri
              :checked selected?
              :on-change #(rf/dispatch [:ui.facets.current/toggle-selection option])}
       disabled? (merge {:disabled true}))]))

(declare code-tree)

(defn- code-item [{:keys [ook/uri label children] :as code}]
  (let [expanded? @(rf/subscribe [:ui.facets.current/code-expanded? uri])]
    [:li.list-group-item.border-0.pb-0
     (when (seq children)
       [toggle-code-expanded-button code expanded?])
     [checkbox-input code]
     [:label.form-check-label.d-inline {:for uri} label]
     (when (seq children)
       [:<>
        [select-all-children-button code]
        (when expanded?
          [code-tree children])])]))

(defn- code-tree [tree]
  [:ul.list-group-flush
   (for [{:keys [ook/uri label] :as code} tree]
     ^{:key [uri label]} [code-item code])])

(defn- codelist-item [{:keys [ook/uri label children] :as codelist}]
  (let [expanded? @(rf/subscribe [:ui.facets.current/code-expanded? uri])]
    [:li.list-group-item.border-0.pb-0
     [toggle-codelist-expanded-button codelist expanded?]
     [checkbox-input codelist]
     [:label.form-check-label.d-inline {:for uri} label]
     [select-any-button codelist]
     (when expanded?
       [code-tree children])]))

(defn- code-selection [{:keys [codelists]}]
  [:<>
   [:p.h6.mt-4 "Codelists"]
   [:form.mt-3
    [:ul.list-group-flush.p-0
     (for [{:keys [ook/uri label] :as codelist} codelists]
       ^{:key [uri label]} [codelist-item codelist])]]])

(defn- no-codelist-message [{:keys [dimensions]}]
  [:<>
   [:p.h6.mt-4 "No codelists for dimensions: "]
   [:ul
    (for [dim dimensions]
      ^{:key dim} [:li dim])]])

(defn- codelist-selection [selected-facet]
  (when selected-facet
    (cond
      (= :loading selected-facet)
      [:div.mt-4.spinner-border {:role "status"}
       [:span.visually-hidden "Loading..."]]

      (= :error selected-facet)
      [:div.alert.alert-danger.mt-3 "Sorry, there was an error fetching the codes for this facet."]

      (seq (:codelists selected-facet))
      [:<>
       [apply-filter-button selected-facet]
       [code-selection selected-facet]]

      (and (:codelists selected-facet) (empty? (:codelists selected-facet)))
      [no-codelist-message selected-facet])))

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
      [codelist-selection selected-facet]]]))
