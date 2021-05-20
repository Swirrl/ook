(ns ook.reframe.codes.browse.view
  (:require
   [re-frame.core :as rf]
   [ook.ui.icons :as icons]
   [ook.ui.common :as common]
   [ook.reframe.codes.view :as codes]))

(defn- toggle-level-button [opts expanded?]
  [:button.btn.as-link.p-0.m-0.me-2.position-relative
   (merge opts
          {:type "button"
           :aria-expanded expanded?
           :style {:top "-4px"
                   :width "1.3rem"}})
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
         {:on-click (fn [] (if all-selected?
                             (rf/dispatch [:ui.event/set-selection :remove-children code])
                             (rf/dispatch [:ui.event/set-selection :add-children code])))}
         (if all-selected? "none" "all children")]))))

(defn- select-any-button [{:keys [ook/uri label] :as codelist}]
  [common/text-button
   {:on-click #(rf/dispatch [:ui.event/set-selection :any codelist])}
   ^{:key uri}[:span "any" [:span.visually-hidden (str label " codes")]]])

(defn- no-codes-message []
  [codes/nested-list
   [codes/nested-list-item {:class "text-muted"}
    [:em "No codes to show"]]])

(declare code-tree)

(defn- code-item [{:keys [children ook/uri] :as code}]
  (let [expanded? @(rf/subscribe [:ui.facets.current/option-expanded? uri])]
    [codes/nested-list-item
     (when (seq children)
       [toggle-code-expanded-button code expanded?])
     [codes/checkbox-input code]
     (when (seq children)
       [:<>
        [select-all-children-button code]
        (when expanded?
          [code-tree children])])]))

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
          [code-tree children])])]))

(defn code-selection [facet-name]
  (let [codelists @(rf/subscribe [:ui.facets/codelists facet-name])]
    [:form
     [:legend.visually-hidden (str "Codes available in the " facet-name " facet")]
     [:fieldset
      [:ul.top-level
       (for [{:keys [ook/uri label] :as codelist} codelists]
         ^{:key [uri label]} [codelist-item codelist])]]]))
