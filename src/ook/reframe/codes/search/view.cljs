(ns ook.reframe.codes.search.view
  (:require
   [re-frame.core :as rf]
   [ook.ui.common :as common]))

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
    [:button.btn.btn-primary {:type "submit"} "Search"]]
   (when (= :error @(rf/subscribe [:ui.facets.current/search-status]))
     [common/error-message "Sorry, there was an error submitting your search"])])
