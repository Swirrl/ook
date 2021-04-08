(ns ook.reframe.views.filters
  (:require
   [re-frame.core :as rf]
   [ook.ui.icons :as icons]
   [clojure.string :as str]
   [ook.util :as u]))

#_(defn filters []
    (let [codes @(rf/subscribe [:results.codes/data])
          query @(rf/subscribe [:results.codes/query])
          cnt (count codes)]
      (when codes
        (list
         ^{:key "answer"}
         [:p "Found " [:strong cnt (u/pluralize " code" cnt)] " matching " [:strong "\"" query "\"."]]
         (when (seq codes)
           [:p "Select all the codes that you're interested in."])
         ^{:key "data"}
         [:form {:id "codes" :on-submit apply-code-selection}
          (doall
           (for [{code-id :code/id dim-id :dim/id label :code/label} codes
                 :let [value (str dim-id "," code-id)]]
             ^{:key code-id} [:div.form-check.mb-3.bg-light
                              [:div.p-2
                               [:input.form-check-input
                                {:type "checkbox"
                                 :name "code"
                                 :value value
                                 :checked (-> @(rf/subscribe [:ui.codes/selection]) (get value))
                                 :id code-id
                                 :on-change #(rf/dispatch [:ui.codes/toggle-selection
                                                           (-> % .-target .-value)])}]
                               [:label.form-check-label {:for code-id}
                                [:strong (single-label label)]
                                [:p.m-0 "id: " [:code code-id]]]]]))
          (when (seq codes)
            [:button.btn.btn-primary.mt-2.mb-4 {:type "submit"} "Find datasets that use these codes"])]))))

(defn- apply-filter-button [{:keys [tree selection]}]
  [:button.btn.btn-primary.mt-3
   {:type "button"
    :disabled (or (not (seq tree)) (not (seq selection)))
    :on-click #(rf/dispatch [:filters/add-current-facet])}
   "Apply filter"])

(defn- toggle-expanded-button [uri expanded?]
  [:button.btn.as-link.p-0.m-0.expand-code-button
   {:on-click #(rf/dispatch [:ui.facets.current/toggle-expanded uri])
    :type "button"}
   (if expanded? icons/down icons/up)])

(defn- multi-select-button [{:keys [ook/uri allow-any? children]}]
  (when (seq children)
    [:button.btn.btn-link.mx-1.p-0.align-baseline
     {:type "button"
      :on-click #(rf/dispatch [:ui.facets.current/set-selection (if allow-any? :any :children) uri])}
     (if allow-any? "any" "all children")]))

(declare code-list)

(defn- code-list-item [{:keys [ook/uri label children disabled?] :as code}]
  (let [expanded? @(rf/subscribe [:ui.facets.current/code-expanded? uri])
        selected? @(rf/subscribe [:ui.facets.current/codelist-selected? uri])]
    [:li.list-group-item.border-0.pb-0
     (when children
       [toggle-expanded-button uri expanded?])
     [:input.form-check-input.mx-2
      (cond-> {:type "checkbox"
               :name "code"
               :value uri
               :id uri
               :checked selected?
               :on-change #(rf/dispatch [:ui.facets.current/toggle-selection (-> % .-target .-value)])}
        disabled? (merge {:disabled true}))]
     [:label.form-check-label.d-inline {:for uri} label]
     [multi-select-button code]
     (when (and expanded? children)
       [code-list children])]))

(defn- code-list [tree & top-level?]
  [:ul.list-group-flush (when top-level? {:class "p-0"})
   (for [{:keys [ook/uri label] :as code} tree]
     ^{:key [uri label]} [code-list-item code])])

(defn- code-selection [{:keys [tree]}]
  [:<>
   [:p.h6.mt-4 "Codelists"]
   [:form.mt-3
    [code-list tree :top-level]]])

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

      (seq (:tree selected-facet))
      [:<>
       [apply-filter-button selected-facet]
       [code-selection selected-facet]]

      (and (:tree selected-facet) (empty? (:tree selected-facet)))
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
    [:div.card.my-4
     [:div.card-body
      [:h2.h5.card-title.me-2.d-inline "Find data"]
      [:span.text-muted "Add a filter"]
      [:div.mt-3.d-flex.align-items-center.justify-content-between
       [:div
        (for [{:keys [name] :as facet} facets]
          (when-not (get applied-facets name)
            ^{:key name}[facet-button facet selected-facet]))]
       (when selected-facet
         [cancel-facet-selection])]
      [codelist-selection selected-facet]]]))
