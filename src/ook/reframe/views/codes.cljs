(ns ook.reframe.views.codes
  (:require
   [re-frame.core :as rf]
   [ook.ui.icons :as icons]
   [ook.ui.common :as common]))

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

(defn- toggle-codelist-expanded-button [current-facet {:keys [ook/uri] :as codelist} expanded?]
  [toggle-level-button
   {:on-click (fn []
                (if (:children codelist)
                  (rf/dispatch [:ui.facets.current/toggle-expanded uri])
                  (rf/dispatch [:facets.codes/get-codes current-facet uri])))}
   expanded?])

(defn- select-any-button [codelist]
  [text-button
   {:on-click #(rf/dispatch [:ui.facets.current/set-selection :any codelist])}
   "any"])

(defn- select-all-children-button [code]
  (let [all-selected? @(rf/subscribe [:ui.facets.current/all-children-selected? code])]
    [text-button
     {:on-click (fn [] (if all-selected?
                    (rf/dispatch [:ui.facets.current/set-selection :remove-children code])
                    (rf/dispatch [:ui.facets.current/set-selection :add-children code])))}
     (if all-selected? "none" "all children")]))

(defn- checkbox-input [{:keys [ook/uri used] :as option}]
  (let [selected? @(rf/subscribe [:ui.facets.current/option-selected? option])]
    [:input.form-check-input.mx-2
     (cond-> {:type "checkbox"
              :name "code"
              :value uri
              :id uri
              :checked selected?
              :on-change #(rf/dispatch [:ui.facets.current/toggle-selection option])}
       (not used) (merge {:disabled true}))]))

(defn- nested-list [opts & children]
  [:ul.list-group-flush opts (common/with-react-keys children)])

(defn- nested-list-item [opts & children]
  [:li.list-group-item.border-0.pb-0 opts (common/with-react-keys children)])

(declare code-tree)

(defn- code-item [{:keys [ook/uri label children] :as code}]
  (let [expanded? @(rf/subscribe [:ui.facets.current/code-expanded? uri])]
    [nested-list-item
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
  [nested-list
   (for [{:keys [ook/uri scheme] :as code} tree]
     ^{:key [scheme uri]} [code-item code])])

(defn- no-codes-message []
  [nested-list
   [nested-list-item {:class "text-muted"}
    [:em "No codes to show"]]])

(defn- codelist-item [current-facet {:keys [ook/uri label children] :as codelist}]
  (let [expanded? @(rf/subscribe [:ui.facets.current/code-expanded? uri])]
    [nested-list-item
     [toggle-codelist-expanded-button current-facet codelist expanded?]
     [checkbox-input (assoc codelist :used true)]
     [:label.form-check-label.d-inline {:for uri} label]
     [select-any-button codelist]
     (when expanded?
       (cond
         (= :loading children)
         [nested-list [nested-list-item (common/loading-spinner)]]

         (= :error children)
         [nested-list
          [nested-list-item
           [:div.alert.alert-danger.mt-3 "Sorry, there was an error fetching the codes for this codelist."]]]

         (= :no-children children)
         [no-codes-message]

         :else
         [code-tree children]))]))

(defn- code-selection [{:keys [codelists] :as current-facet}]
  [:<>
   [:p.h6.mt-4 "Codelists"]
   [:form.mt-3
    [nested-list {:class "p-0"}
     (for [{:keys [ook/uri label] :as codelist} (->> codelists vals (sort-by :ook/uri))]
       ^{:key [uri label]} [codelist-item current-facet codelist])]]])

(defn- no-codelist-message [{:keys [dimensions]}]
  [:<>
   [:p.h6.mt-4 "No codelists for dimensions: "]
   [:ul
    (for [dim dimensions]
      ^{:key dim} [:li dim])]])

(defn codelist-selection [selected-facet]
  (when selected-facet
    (cond
      (= :loading selected-facet)
      [:div.mt-4.ms-1 [common/loading-spinner]]

      (= :error selected-facet)
      [:div.alert.alert-danger.mt-3 "Sorry, there was an error fetching the codelists for this facet."]

      (seq (:codelists selected-facet))
      [:<>
       [apply-filter-button selected-facet]
       [code-selection selected-facet]]

      (and (:codelists selected-facet) (empty? (:codelists selected-facet)))
      [no-codelist-message selected-facet])))
