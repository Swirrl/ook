(ns ook.reframe.codes.search.view
  (:require
   [re-frame.core :as rf]
   [ook.ui.common :as common]
   [ook.reframe.codes.view :as codes]
   [ook.ui.icons :as icons]))

(defn- submit-search [event]
  (.preventDefault event)
  (rf/dispatch [:ui.event/submit-code-search]))

(defn code-search []
  [:<>
   [:form.d-flex.my-3 {:id "search" :on-submit submit-search}
    [:input.form-control.form-control.lg.me-2
     {:id "search-term"
      :name "search-term"
      :type "search"
      :placeholder "Search codes by name"
      :aria-label "Search"
      :value @(rf/subscribe [:ui.facets.current/search-term])
      :on-change #(rf/dispatch [:ui.event/search-term-change (-> % .-target .-value)])}]
    [common/primary-button {:type "submit"} "Search"]]])

(defn- options [search-results]
  [:<>
   (when (seq search-results)
     [common/text-button
      {:on-click #(rf/dispatch [:ui.event/select-all-matches])}
      "select all matches"])
   [common/text-button
    {:on-click #(rf/dispatch [:ui.event/reset-search])}
    "reset search"]])

(declare code-tree)

(defn- code-item [{:keys [children label ook/uri] :as code}]
  [codes/nested-list-item
   (if @(rf/subscribe [:ui.facets.current/search-result? uri])
     [codes/checkbox-input code]
     [:span icons/bullet label ])
   (when (seq children)
     [code-tree children])])

(defn- code-tree [tree]
  [codes/nested-list
   (for [{:keys [ook/uri scheme] :as code} tree]
     ^{:key [scheme uri]} [code-item code])])

(defn- codelist-item [{:keys [children label ook/uri]}]
  [codes/nested-list-item
   [:span icons/bullet label]
   [codes/codelist-wrapper uri
    [code-tree children]]])

(defn- search-results-tree [codelists]
  [codes/top-tree-level
   (for [{:keys [ook/uri label] :as codelist} codelists]
     ^{:key [uri label]} [codelist-item codelist])])

(defn- search-results [codelists]
  (let [search-results @(rf/subscribe [:ui.facets.current/search-results])]
    [:<>
     [options search-results]
     (if (empty? search-results)
       [:p.mt-3.ms-1 [:em.text-muted "No codes match"]]
       [search-results-tree codelists])]))

(defn search-info [codelists]
  (let [search-status @(rf/subscribe [:ui.facets.current/search-status])]
    (condp = search-status
      :loading [:div.ms-1 [common/loading-spinner]]

      :ready [search-results codelists]

      :error [common/error-message "Sorry, there was an error submitting your search"]

      [common/error-message "Sorry, something went wrong."])))
