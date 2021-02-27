(ns ook.ui.error-boundary
  (:require [reagent.core :as r]))

(defn error-boundary [& _]
  (let [error (r/atom nil)]
    (r/create-class
     {:display-name "ErrorBoundary"

      :get-derived-state-from-error (fn [e]
                                      (reset! error e))

      :reagent-render (fn [& children]
                        (if @error
                          [:div
                           [:strong "Sorry!"]
                           [:p [:span "Something went wrong. "
                                [:button.as-link {:on-click #(reset! error nil)}
                                 "Try again"]]]]

                          (into [:<>] children)))})))
