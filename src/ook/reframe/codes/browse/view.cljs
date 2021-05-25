(ns ook.reframe.codes.browse.view
  (:require
   [re-frame.core :as rf]
   [ook.ui.icons :as icons]
   [ook.ui.common :as common]
   [ook.reframe.codes.view :as codes]))

(def toggle-button-dimension "1.3rem")

(defn- toggle-level-button [opts expanded?]
  [:button.btn.as-link.p-0.m-0.position-relative.me-2.d-inline
   (merge opts
          {:type "button"
           :aria-expanded expanded?
           :style {:top "-4px" :width toggle-button-dimension}})
   (if expanded? icons/down icons/up)])

(defn- toggle-code-expanded-button [{:keys [ook/uri label]} expanded?]
  [toggle-level-button
   {:on-click #(rf/dispatch [:ui.event/toggle-disclosure uri])
    :aria-label (str "Toggle disclosure of " label)}
   expanded?])

(defn- toggle-codelist-expanded-button [{:keys [ook/uri label]} expanded?]
  [toggle-level-button
   {:on-click #(rf/dispatch [:ui.event/toggle-codelist uri])
    :aria-label (str "Toggle disclosure of " label)}
   expanded?])

(defn- select-all-children-button [code]
  (let [any-used-children? @(rf/subscribe [:ui.facets.current/any-used-children? code])]
    (when any-used-children?
      (let [all-selected? @(rf/subscribe [:ui.facets.current/all-used-children-selected? code])]
        [common/text-button
         {:aria-label (str (when all-selected? "un-")
                           "select all children of " (:label code))
          :on-click (fn [] (if all-selected?
                             (rf/dispatch [:ui.event/set-selection :remove-children code])
                             (rf/dispatch [:ui.event/set-selection :add-children code])))}
         (if all-selected? "none" "all children")]))))

(defn- select-any-button [{:keys [ook/uri label] :as codelist}]
  [common/text-button
   {:on-click #(rf/dispatch [:ui.event/set-selection :any codelist])}
   ^{:key uri} [:span "any" [:span.visually-hidden (str label " codes")]]])

(defn- no-codes-message []
  [codes/nested-list
   [codes/nested-list-item {:class "text-muted"}
    [:em "No codes to show"]]])

(defn- spacer
  "An empty div with the same dimensions as the toggle expand button so that all checkboxes
   align at the same level, even if they're not expandable"
  []
  [:div.me-2 {:style {:display "inline-block"
                      :width toggle-button-dimension}}])

(declare code-tree)

(defn- code-item [{:keys [children ook/uri] :as code}]
  (let [expanded? @(rf/subscribe [:ui.facets.current/option-expanded? uri])]
    [codes/nested-list-item
     [:div.form-check
      [:span {:style {:margin-left "-3.1rem"}}
       (if (seq children)
         [toggle-code-expanded-button code expanded?]
         [spacer])]
      [:span {:style {:margin-left "1.3rem"}}
       [codes/checkbox-input code]]
      (when (seq children)
        [select-all-children-button code])]
     (when expanded?
       [code-tree children])]))

(defn- code-tree [tree]
  [codes/nested-list
   (for [{:keys [ook/uri scheme] :as code} tree]
     ^{:key [scheme uri]} [code-item code])])

(defn- codelist-item [{:keys [children ook/uri] :as codelist}]
  (let [expanded? @(rf/subscribe [:ui.facets.current/option-expanded? uri])]
    [codes/nested-list-item
     [toggle-codelist-expanded-button codelist expanded?]
     [codes/checkbox-input (assoc codelist :used true)]
     [select-any-button codelist]
     (when expanded?
       [codes/codelist-wrapper uri
        (if (= :no-children children)
          [no-codes-message]
          [:div {:style {:margin-left "1.5rem"}}
           [code-tree children]])])]))

(defn code-selection [facet-name]
  (let [codelists @(rf/subscribe [:ui.facets/codelists facet-name])]
    [:form
     [:legend.visually-hidden (str "Codes available in the " facet-name " facet")]
     [:fieldset
      [:ul.top-level
       (for [{:keys [ook/uri label] :as codelist} codelists]
         ^{:key [uri label]} [codelist-item codelist])]]]))
