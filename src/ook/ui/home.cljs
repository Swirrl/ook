(ns ook.ui.home
  (:require [ook.ui.common :as c]))

(defn ui [state]
  [c/loading-wrapper state
   [:h1 "Hello Swirrld"]
   [:p "This is a reagent component"]
   [:p @state]])
