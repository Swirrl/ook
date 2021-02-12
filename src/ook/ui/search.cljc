(ns ook.ui.search
  (:require [ook.ui.common :as c]
            ;; [ook.ui.state :as state]
            [clojure.string :as str]
            #?@(:cljs [[ook.handler :as handler]])))

(defn search-form [current-val]
  [:form.d-flex.mt-4.mb-4
   #?(:clj {:id "search" :action "/search" :method "GET" :style {:max-width "44rem"}}
      :cljs {:id "search" :on-submit handler/submit-search :style {:max-width "44rem"}})
   [:input.form-control.form-control.lg.me-2
    {:id "query"
     :name "q"
     :type "search"
     :placeholder "Search"
     :default-value (:query @current-val)
     :aria-label "Search"}]
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
   (search-form state)
   (results state)))


;; (if-let [match @state/match]
;;   (let [view (-> match :data :view)]
;;     (println match)
;;     [view match]))
