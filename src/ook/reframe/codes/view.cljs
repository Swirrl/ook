(ns ook.reframe.codes.view
  (:require
   [re-frame.core :as rf]
   [ook.ui.icons :as icons]
   [ook.ui.common :as common]
   [ook.reframe.codes.search.view :as search]))

(defn- apply-filter-button []
  (let [current-selection @(rf/subscribe [:ui.facets.current/selection])]
    [common/primary-button
     {:class "mt-3"
      :disabled (empty? current-selection)
      :on-click #(rf/dispatch [:ui.event/apply-current-facet])}
     "Apply filter"]))

(defn- toggle-level-button [opts expanded?]
  [:button.btn.as-link.p-0.m-0.expand-code-button
   (merge opts {:type "button"})
   (if expanded? icons/down icons/up)])

(defn- toggle-code-expanded-button [uri expanded?]
  [toggle-level-button
   {:on-click #(rf/dispatch [:ui.event/toggle-disclosure uri])}
   expanded?])

(defn- toggle-codelist-expanded-button [uri expanded?]
  [toggle-level-button
   {:on-click #(rf/dispatch [:ui.event/toggle-codelist uri])}
   expanded?])

(defn- select-any-button [codelist]
  [common/text-button
   {:on-click #(rf/dispatch [:ui.event/set-selection :any codelist])}
   "any"])

(defn- select-all-children-button [code]
  (let [all-selected? @(rf/subscribe [:ui.facets.current/all-children-selected? code])]
    [common/text-button
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
               :checked selected?
               :on-change #(rf/dispatch [:ui.event/toggle-selection option])}
        (not used) (merge {:disabled true}))]
     [:label.form-check-label.d-inline {:for id} label]]))

(defn- nested-list [opts & children]
  [:ul.list-group-flush opts (common/with-react-keys children)])

(defn- nested-list-item [opts & children]
  [:li.list-group-item.border-0.pb-0 opts (common/with-react-keys children)])

(declare code-tree)

(defn- code-item [{:keys [children ook/uri] :as code}]
  (let [expanded? @(rf/subscribe [:ui.facets.current/option-expanded? uri])]
    [nested-list-item
     (when (seq children)
       [toggle-code-expanded-button uri expanded?])
     [checkbox-input code]
     (when (seq children)
       [:<>
        [select-all-children-button code]
        (when expanded?
          [code-tree children])])]))

(defn- no-codes-message []
  [nested-list
   [nested-list-item {:class "text-muted"}
    [:em "No codes to show"]]])

(defn- code-tree [tree]
  [nested-list
   (for [{:keys [ook/uri scheme] :as code} tree]
     ^{:key [scheme uri]} [code-item code])])

(defn- codelist-item [{:keys [children ook/uri] :as codelist}]
  (let [expanded? @(rf/subscribe [:ui.facets.current/option-expanded? uri])]
    [nested-list-item
     [toggle-codelist-expanded-button uri expanded?]
     [checkbox-input (assoc codelist :used true)]
     [select-any-button codelist]
     (when expanded?
       (let [status @(rf/subscribe [:ui.codes/status uri])]
         (condp = status
           :loading
           [nested-list [nested-list-item (common/loading-spinner)]]

           :error
           [nested-list
            [nested-list-item
             [common/error-message "Sorry, there was an error fetching the codes for this codelist."]]]

           :ready
           (if (= :no-children children)
             [no-codes-message]
             [code-tree children])

           [:div])))]))

(defn- code-selection [codelists]
  [:form.mt-3
   [nested-list {:class "p-0"}
    (for [{:keys [ook/uri label] :as codelist} codelists]
      ^{:key [uri label]} [codelist-item codelist])]])

(defn- search-info [codelists search-results]
  (let [search-status @(rf/subscribe [:ui.facets.current/search-status name])]
    (condp = search-status
      :ready
      [:<>
       [search/options search-results]
       (if (empty? search-results)
         [:p.mt-3.ms-1 [:em.text-muted "No codes match"]]
         [code-selection codelists])]

      :error [common/error-message "Sorry, there was an error searching for codes"]

      :loading [common/loading-spinner]

      [common/error-message "Sorry, something went wrong."])))

(defn- codelists [name]
  (when name
    ;; TODO:: filter down these codelists in the sub?
    ;; or do it in the view? check each iteration if it's included in the selection
    (let [codelists @(rf/subscribe [:facets.config/codelists name])
          search-results @(rf/subscribe [:ui.facets.current/search-results name])]
      (if (= :no-codelists codelists)
        [:p.h6.mt-4 "No codelists for facet"]
        [:<>
         [apply-filter-button]
         [:p.h6.mt-4 "Codelists"]
         [search/code-search]
         (if search-results
           [search-info codelists search-results]
           [code-selection codelists])]))))

(defn codelist-selection [selected-facet-status facet-name]
  (when selected-facet-status
    (condp = selected-facet-status
      :loading [:div.mt-4.ms-1 [common/loading-spinner]]

      :error [common/error-message "Sorry, there was an error fetching the codelists for this facet."]

      :ready [codelists facet-name]

      [common/error-message "Sorry, something went wrong."])))
