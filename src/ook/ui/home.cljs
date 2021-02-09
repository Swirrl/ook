(ns ook.ui.home)

(defn ui [state]
  [:div
   [:h1 "Hello Swirrld"]
   [:p "This is a reagent component"]
   [:p @state]])
