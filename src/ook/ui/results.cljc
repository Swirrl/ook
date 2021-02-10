(ns ook.ui.results)

(defn search-results [{:keys [count results query]}]
  (list
   [:h2 "Found " count " codes matching \"" query "\":"]
   [:ul
    (for [result results]
      [:li (for [[k v] result]
             [:p [:strong k ": "] v])])]))
