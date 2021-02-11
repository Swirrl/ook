(ns ook.ui.home
  (:require [ook.ui.common :as c]))

(defn- search-form []
  [:form.d-flex.mt-4 {:method "GET" :action "/search" :style {:max-width "50rem"}}
   [:input.form-control.form-control.lg.me-2
    {:id "query"
     :name "q"
     :type "search"
     :placeholder "Search"
     :aria-label "Search"}]
   [:button.btn.btn-primary {:type "submit"} "Search"]])

(defn- results [state]
  [:div "Results go here:"
   [:div @state]])

(defn ui [state]
  [c/loading-wrapper state
   [:h1 "Search for a code"]
   (search-form)
   (results state)])
