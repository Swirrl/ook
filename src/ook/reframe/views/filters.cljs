(ns ook.reframe.views.filters
  (:require
   [re-frame.core :as rf]
   [clojure.string :as str]
   [ook.util :as u]))

;; (defn- single-label [label]
;;   (if (coll? label)
;;     (str/join ", " label)
;;     label))

;; (defn- apply-code-selection [event]
;;   (.preventDefault event)
;;   (rf/dispatch [:app/navigate :ook.route/search]))

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

(defn- apply-filters []
  (rf/dispatch [:filters/add-current-facet])
  (rf/dispatch [:app/navigate :ook.route/search]))

(defn- codelist-selection [selected-facet]
  (when selected-facet
    (let [codelists @(rf/subscribe [:ui.facets/current-codelists])]
      [:<>
       [:button.btn.btn-primary.mt-3
        {:type "button"
         :disabled (or (not (seq codelists)) (not (seq (:selection selected-facet))))
         :on-click apply-filters}
        "Apply filter"]
       (if (seq codelists)
         [:<>
          [:p.h6.mt-4 "Codelists"]
          [:form.mt-3
           (doall
            (for [{:keys [id label]} codelists]
              ^{:key id}
              [:div.form-check.mb-3.bg-light
               [:div.p-2
                [:input.form-check-input
                 {:type "checkbox"
                  :name "codelist"
                  :value id
                  :id id
                  :checked (-> @(rf/subscribe [:ui.facets.current/codelist-selected? id]))
                  :on-change #(rf/dispatch [:ui.facets.current/toggle-selection (-> % .-target .-value)])}]
                [:label.form-check-label {:for id}
                 [:strong label]
                 [:p.m-0 "id: " [:code id]]]]]))]]
         [:<>
          [:p.h6.mt-4 "No codelists for dimensions: "]
          [:ul
           (for [dim (:dimensions selected-facet)]
             ^{:key dim} [:li dim])]])])))

(defn- facet-button [{:keys [name] :as facet} selected-facet]
  ^{:key name}
  [:button.btn.me-2
   {:type "button"
    :class (if (= name (:name selected-facet)) "btn-dark" "btn-outline-dark")
    :on-click #(rf/dispatch [:ui.facets/set-current facet])}
   name])

(defn- cancel-facet-selection []
  [:button.btn-close.border.border-dark
   {:type "button"
    :aria-label "Close filter selection"
    :on-click #(rf/dispatch [:ui.facets/set-current nil])}])

(defn configured-facets [facets]
  (let [selected-facet @(rf/subscribe [:ui.facets/current])
        applied-facets @(rf/subscribe [:facets/applied])]
    [:div.card.my-4
     [:div.card-body
      [:h2.h5.card-title.me-2.d-inline "Find data"]
      [:span.text-muted "Add a filter"]
      [:div.mt-3.d-flex.align-items-center.justify-content-between
       [:div
        (for [{:keys [name] :as facet} facets]
          (when-not (get applied-facets name)
            (facet-button facet selected-facet)))]
       (when selected-facet
         (cancel-facet-selection))]
      (codelist-selection selected-facet)]]))
