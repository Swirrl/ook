(ns ook.reframe.views.search
  (:require
   [re-frame.core :as rf]))

(defn- search-form []
  [:form.d-flex.my-3 {:id "search" :on-submit #(rf/dispatch [:codes/submit-search])}
   [:input.form-control.form-control.lg.me-2
    {:id "query"
     :name "q"
     :type "search"
     :placeholder "Search"
     :aria-label "Search"
     :value @(rf/subscribe [:ui.codes/query])
     :on-change #(rf/dispatch [:ui.codes/query-change (-> % .-target .-value)])}]
   [:button.btn.btn-primary {:type "submit"} "Search"]])

(defn create-filter-card [& children]
  [:div.card
   [:div.card-header
    [:h2.d-inline.h5.me-2 "Create a custom filter"]
    [:span.text-muted "search for a code"]
    (search-form)]
   (when (seq children)
     [:div.card-body
      children])])

(defn home []
  (create-filter-card))

(defn results []
  (let [codes @(rf/subscribe [:results.codes/data])]
    (create-filter-card
      "codes here..."
      ;; do something with the codes...
      )
    ))
