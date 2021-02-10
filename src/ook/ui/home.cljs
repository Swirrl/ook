(ns ook.ui.home
  (:require [ook.ui.common :as c]))

(defn- search-form []
  [:form.d-flex {:method "GET" :action "/search"}
   [:input.form-control.form-control.lg.me-2
    {:id "query"
     :name "query"
     :type "search"
     :placeholder "Search"
     :aria-label "Search"}]
   [:button.btn.btn-primary {:type "submit"} "Search"]])

(defn- results [state]
  [:div "Results go here:"
   [:div @state]])

(defn ui [state]
  [c/loading-wrapper state
   [:h1 "ONS Trade Search"]
   [:p "This is a reagent component"]
   (search-form)
   (results state)])
