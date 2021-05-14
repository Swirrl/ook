(ns ook.reframe.codes.search.view
  (:require
   [re-frame.core :as rf]
   [ook.ui.common :as common]
   [ook.reframe.codes.view :as codes]
   [ook.ui.icons :as icons]
   [ook.util :as u]))

(defn- submit-search [event]
  (.preventDefault event)
  (rf/dispatch [:ui.event/submit-code-search]))

(defn code-search []
  [:<>
   [:form.d-flex.mb-3.mt-4 {:id "search" :on-submit submit-search}
    [:input.form-control.form-control.lg.me-2
     {:id "search-term"
      :name "search-term"
      :type "search"
      :placeholder "Search codes by name"
      :aria-label "Search"
      :style {:max-width "41rem"}
      :value @(rf/subscribe [:ui.facets.current/search-term])
      :on-change #(rf/dispatch [:ui.event/search-term-change (-> % .-target .-value)])}]
    [common/primary-button {:type "submit" :aria-label "Submit search"}
     [:span.d-block icons/search]]]])

(defn- options [any-results?]
  [:div.my-4
   (when any-results?
     [:<>
      [common/text-button
       {:on-click #(rf/dispatch [:ui.event/select-all-matches])
        :disabled @(rf/subscribe [:ui.search/all-matches-selected?])}
       "Select all matches"]
      [common/text-button
       {:on-click #(rf/dispatch [:ui.event/unselect-all-matches])
        :disabled (not @(rf/subscribe [:ui.search/any-matches-selected?]))}
       "Un-select all matches"]])
   [common/text-button
    {:on-click #(rf/dispatch [:ui.event/reset-search])}
    "Reset search"]])

(declare code-tree)

(defn- code-item [{:keys [children label ook/uri] :as code}]
  [codes/nested-list-item
   (if @(rf/subscribe [:ui.facets.current/search-result? uri])
     [codes/checkbox-input code]
     [:span label])
   (when (seq children)
     [code-tree children])])

(defn- code-tree [tree]
  [codes/nested-list
   (for [{:keys [ook/uri scheme] :as code} tree]
     ^{:key [scheme uri]} [code-item code])])

(defn- codelist-item [{:keys [children label ook/uri]}]
  [codes/nested-list-item
   [:h4.mt-3 label]
   [codes/codelist-wrapper uri
    [code-tree children]]])

(defn- results-count-message []
  (let [code-count @(rf/subscribe [:ui.facets.current/search-result-code-count])
        codelist-count @(rf/subscribe [:ui.facets.current/search-result-codelist-count])]
    [:h3 (str "Found " code-count (u/pluralize " code" code-count)
                  " in " codelist-count (u/pluralize " codelist" codelist-count))]))

(defn- search-results-tree [facet-name]
  (let [codelists @(rf/subscribe [:ui.facets.current/filtered-codelists facet-name])]
    [:<>
     [results-count-message]
     [:ul.top-level.search-results.ms-1
      (for [{:keys [ook/uri label] :as codelist} codelists]
        ^{:key [uri label]} [codelist-item codelist])]]))

(defn- search-results [facet-name]
  (let [any-results? @(rf/subscribe [:ui.facets.current/any-results?])]
    [:<>
     [options any-results?]
     (if any-results?
       [search-results-tree facet-name]
       [:p.mt-3.ms-1 [:em.text-muted "No codes match"]])]))

(defn search-info [facet-name]
  (let [search-status @(rf/subscribe [:ui.facets.current/search-status])]
    (condp = search-status
      :loading [:div.ms-1 [common/loading-spinner]]

      :ready [search-results facet-name]

      :error [common/error-message "Sorry, there was an error submitting your search"]

      [common/error-message "Sorry, something went wrong."])))
