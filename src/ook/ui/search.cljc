(ns ook.ui.search
  (:require [ook.ui.common :as c]
            [clojure.string :as str]
            #?@(:cljs [[ook.handler :as handler]])))

(defn search-form [state]
  [:form.d-flex.mt-4.mb-4
   #?(:clj {:id "search" :action "/search" :method "GET" :style {:max-width "44rem"}}
      :cljs {:id "search" :on-submit handler/submit-search :style {:max-width "44rem"}})
   [:input.form-control.form-control.lg.me-2
    (-> {:id "query"
         :name "q"
         :type "search"
         :placeholder "Search"
         :aria-label "Search"
         :default-value (or (:query @state) "")})]
   [:button.btn.btn-primary {:type "submit"} "Search"]])

(defn results [state]
  (let [{:keys [count results query] :as current-state} @state]
    (if current-state
      (c/siblings
       [:h2 "Found " count " codes matching \"" query "\":"]
       [:ul
        (for [result results]
          ^{:key (:id result)}
          [:li (for [[k v] result]
                 ^{:key v}
                 [:p [:strong k ": "]
                  (if (coll? v)
                    (str/join ", " v)
                    v)])])])
      [:div])))

(defn ui [state]
  (c/state-wrapper state
   (c/siblings
    (search-form state)
    (results state))))
