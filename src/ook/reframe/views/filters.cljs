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

;; (defn filters []
;;   (let [codes @(rf/subscribe [:results.codes/data])
;;         query @(rf/subscribe [:results.codes/query])
;;         cnt (count codes)]
;;     (when codes
;;       (list
;;        ^{:key "answer"}
;;        [:p.m-0 "Found " [:strong cnt (u/pluralize " code" cnt)] " matching " [:strong "\"" query "\""]]
;;        ^{:key "data"}
;;        [:form {:id "codes" :on-submit apply-code-selection}
;;         (when (seq codes)
;;           [:button.btn.btn-primary.mt-2.mb-4 {:type "submit"} "Apply selection"])
;;         (doall
;;          (for [{code-id :code/id dim-id :dim/id label :code/label} codes
;;                :let [value (str dim-id "," code-id)]]
;;            ^{:key code-id} [:div.form-check.mb-3.bg-light
;;                        [:div.p-2
;;                         [:input.form-check-input
;;                          {:type "checkbox"
;;                           :name "code"
;;                           :value value
;;                           :checked (-> @(rf/subscribe [:ui.codes/selection]) (get value))
;;                           :id code-id
;;                           :on-change #(rf/dispatch [:ui.codes/toggle-selection
;;                                                     (-> % .-target .-value)])}]
;;                         [:label.form-check-label {:for code-id}
;;                          [:strong (single-label label)]
;;                          [:p.m-0 "id: " [:code code-id]]]]]))]))))

(defn configured-facets [facets]
  (let [selected-facet @(rf/subscribe [:ui.facets/current])]
    [:div.card
     [:div.card-body
      [:h2.h5.card-title.me-2.d-inline "Find data"]
      [:span.text-muted "Add a filter"]
      [:div.mt-3.d-flex.align-items-center.justify-content-between
       [:div
        (for [{:keys [name] :as facet} facets]
          ^{:key name} [:button.btn.me-2
                        {:type "button"
                         :class (if (= name (:name selected-facet)) "btn-dark" "btn-outline-dark")
                         :on-click #(rf/dispatch [:ui.facets/set-current facet])}
                        name])]
       [:button.btn-close.border.border-dark
        {:type "button"
         :aria-label "Close filter selection"
         :on-click #(rf/dispatch [:ui.facets/set-current nil])}]]
      (when selected-facet
        [:<>
         [:button.btn.btn-primary.mt-3
          {:type "button" :disabled (not (seq (:codelists selected-facet)))}
          "Apply filter"]
         (let [{:keys [codelists]} selected-facet]
           (if (seq codelists)
             [:form.mt-3
              (for [cl codelists]
                ^{:key cl}
                [:div.form-check.mb-3.bg-light
                 [:div.p-2
                  [:input.form-check-input
                   {:type "checkbox"
                    :name "codelist"
                    :value cl
                    :id cl
                    :checked (-> @(rf/subscribe [:ui.facets/current]) :selection (get cl) boolean)
                    :on-change #(rf/dispatch [:ui.facets.current/toggle-selection (-> % .-target .-value)])}]
                  [:label.form-check-label {:for cl}
                   cl]]])]
             [:<>
              [:p.mt-3 [:strong "No codelists for dimensions: "]]
              [:ul
               (for [dim (:dimensions selected-facet)]
                 [:li dim])]]))])]]))
