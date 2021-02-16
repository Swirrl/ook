(ns ook.ui.search
  (:require [ook.ui.common :as c]
            [clojure.string :as str]
            #?@(:cljs [[ook.handler :as handler]
                       [ook.ui.state :as state]])))

#?(:cljs
   (defn reset-search-input! []
     (swap! state/components-state assoc :search nil)
     (set! (.-value  (.getElementById js/document "query")) "")))

(defn search-form [state]
  [:form.d-flex.mt-4.mb-4
   #?(:clj {:id "search" :action "/search" :method "GET"}
      :cljs {:id "search" :on-submit handler/submit-search})
   [:input.form-control.form-control.lg.me-2
    (-> {:id "query"
         :name "q"
         :type "search"
         :placeholder "Search"
         :aria-label "Search"
         :default-value (or (:query @state) "")})]
   [:button.btn.btn-primary {:type "submit"} "Search"]])

(defn- single-label [label]
  (if (coll? label)
    (str/join ", " label)
    label))

(defn- codes [{:keys [result/count result/data result/query]}]
  (c/siblings
   [:p [:strong "Found " count " codes matching \"" query "\""]]
   [:form
    (for [{:keys [id label]} data]
      ^{:key id} [:div.form-check.mb-3.bg-light
                  [:div.p-2
                   [:input.form-check-input {:type "checkbox" :value id :id id}]
                   [:label.form-check-label {:for id}
                    [:strong (single-label label)]
                    [:p.m-0 "id: " [:code id]]]]])]))

(defn results [state]
  (when-let [current-state @state]
    (codes current-state)))

(defn ui [state]
  (c/state-wrapper
   state
   [:div {:style {:max-width "50rem" :margin "0 auto"}}
    [:h1 "Search for a code"]
    (search-form state)
    (results state)]))
