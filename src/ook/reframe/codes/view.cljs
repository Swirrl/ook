(ns ook.reframe.codes.view
  (:require
   [re-frame.core :as rf]
   [ook.ui.icons :as icons]
   [ook.ui.common :as common]))

(defn- apply-filter-button []
  (let [current-selection @(rf/subscribe [:ui.facets.current/selection])]
    [:button.btn.btn-primary.mt-3
     {:type "button"
      :disabled (empty? current-selection)
      :on-click #(rf/dispatch [:ui.event/apply-current-facet])}
     "Apply filter"]))

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
   {:on-click #(rf/dispatch [:ui.event/toggle-disclosure uri])}
   expanded?])

(defn- toggle-codelist-expanded-button [{:keys [ook/uri children]} expanded?]
  [toggle-level-button
   {:on-click (fn []
                (if children
                  (rf/dispatch [:ui.event/toggle-disclosure uri])
                  (rf/dispatch [:ui.event/get-codes uri])))}
   expanded?])

(defn- select-any-button [codelist]
  [text-button
   {:on-click #(rf/dispatch [:ui.event/set-selection :any codelist])}
   "any"])

(defn- select-all-children-button [code]
  (let [all-selected? @(rf/subscribe [:ui.facets.current/all-children-selected? code])]
    [text-button
     {:on-click (fn [] (if all-selected?
                    (rf/dispatch [:ui.event/set-selection :remove-children code])
                    (rf/dispatch [:ui.event/set-selection :add-children code])))}
     (if all-selected? "none" "all children")]))

(defn- checkbox-input [{:keys [ook/uri label used] :as option}]
  (let [selected? @(rf/subscribe [:ui.facets.current/option-selected? option])
        id (str (gensym uri))]
    [:<>
     [:input.form-check-input.mx-2
      (cond-> {:type "checkbox"
               :name "code"
               :value uri
               :id id
               :checked (and used selected?)
               :on-change #(rf/dispatch [:ui.event/toggle-selection option])}
        (not used) (merge {:disabled true}))]
     [:label.form-check-label.d-inline {:for id} label]]))

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

(defn- codelist-item [{:keys [ook/uri children] :as codelist}]
  (let [expanded? @(rf/subscribe [:ui.facets.current/code-expanded? uri])]
    [nested-list-item
     [toggle-codelist-expanded-button codelist expanded?]
     [checkbox-input (assoc codelist :used true)]
     [select-any-button codelist]
     (when expanded?
       (cond
         (= :loading children)
         [nested-list [nested-list-item (common/loading-spinner)]]

         (= :error children)
         [nested-list
          [nested-list-item
           [common/error-message "Sorry, there was an error fetching the codes for this codelist."]]]

         (= :no-children children)
         [no-codes-message]

         :else
         [code-tree children]))]))

(defn- code-selection []
  (let [codelists @(rf/subscribe [:ui.facets.current/codelists])]
    [:<>
     [apply-filter-button]
     [:p.h6.mt-4 "Codelists"]
     [:form.mt-3
      [nested-list {:class "p-0"}
       (for [{:keys [ook/uri label] :as codelist} codelists]
         ^{:key [uri label]} [codelist-item codelist])]]]))

(defn codelist-selection [selected-facet-status]
  (when selected-facet-status
    (condp = selected-facet-status
      :loading [:div.mt-4.ms-1 [common/loading-spinner]]

      :error [common/error-message "Sorry, there was an error fetching the codelists for this facet."]

      :success/empty [:p.h6.mt-4 "No codelists for facet"]

      :success/ready [code-selection]

      [common/error-message "Sorry, something went wrong."])))
