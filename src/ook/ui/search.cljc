(ns ook.ui.search
  (:require [ook.ui.common :as c]
            [clojure.string :as str]
            #?@(:cljs [[ook.ui.state :as state]])))

#?(:cljs
   (defn reset-search-input! []
     (swap! state/components-state assoc :search nil)
     (set! (.-value  (.getElementById js/document "query")) "")))

(defn search-form [state {:keys [handler/submit-search]}]
  [:form.d-flex.mt-4.mb-4
   #?(:clj {:id "search" :action "/search" :method "GET"}
      :cljs {:id "search" :on-submit submit-search})
   [:input.form-control.form-control.lg.me-2
    (-> {:id "query"
         :name "q"
         :type "search"
         :placeholder "Search"
         :aria-label "Search"
         :default-value (or (:result.codes/query @state) "")})]
   [:button.btn.btn-primary {:type "submit"} "Search"]])

(defn- single-label [label]
  (if (coll? label)
    (str/join ", " label)
    label))

(defn- codes [{:keys [result.codes/count result.codes/data result.codes/query]}
              {:keys [handler/apply-code-selection]}]
  (c/siblings
   [:p [:strong "Found " count " codes matching \"" query "\""]]
   [:form {:id "codes"
           :action "/apply-filters" :method "GET"
           ;; :on-submit apply-code-selection
           }
    [:button.btn.btn-primary.mb-3 {:type "submit"} "Find datasets with selected codes"]
    (for [{:keys [id label scheme]} data]
      ^{:key id} [:div.form-check.mb-3.bg-light
                  [:div.p-2
                   [:input.form-check-input {:type "checkbox" :name "code"
                                             :value [id, scheme] :id id}]
                   [:label.form-check-label {:for id}
                    [:strong (single-label label)]
                    [:p.m-0 "id: " [:code id]]]]])]))

(defn- datasets [{:keys [result.datasets/data]} props]
  (when data
    [:table.table
     [:thead
      [:tr
       [:th {:scope "col"} "Dataset Id"]
       [:th {:scopt "col"}]]]

     [:tbody
      (for [{:keys [dataset matching-observations]} data]
        ^{:key dataset} [:tr
                         [:th {:scope "row"} dataset]
                         [:td
                          [:small (str "Found " matching-observations " matching observations")]
                          [:div
                           [:a.btn.btn-secondary.btn-sm {:href "#"} "View Data"]]]])]]))

(defn- results [state props]
  (when-let [current-state @state]
    (c/siblings
     (codes current-state props)
     (datasets current-state props))))

(defn ui [state props]
  (c/state-wrapper
   state
   [:div {:style {:max-width "50rem" :margin "0 auto"}}
    [:h1 "Search for a code"]
    (search-form state props)
    (results state props)]))
