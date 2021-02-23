(ns ook.ui.search
  (:require [ook.ui.common :as c]
            [ook.ui.filters :as filters]
            [ook.ui.results :as res]
            #?@(:cljs [[ook.ui.state :as state]])))

#?(:cljs
   (do
     (defn reset-state! []
       (swap! state/components-state assoc :main nil))

     (defn- update-query [event]
       (swap! state/components-state
              assoc-in
              [:main :ui :codes :query]
              (-> event .-target .-value)))))

(defn- search-form [state {:keys [handler/submit-search]}]
  [:form.d-flex.my-3
   #?(:clj {:id "search" :action "/search" :method "GET"}
      :cljs {:id "search" :on-submit submit-search})
   [:input.form-control.form-control.lg.me-2
    (-> {:id "query"
         :name "q"
         :type "search"
         :placeholder "Search"
         :aria-label "Search"
         :value (-> @state :ui :codes :query (or ""))}
        #?(:cljs (merge {:on-change update-query})))]
   [:button.btn.btn-primary {:type "submit"} "Search"]])

(defn- create-filter-card [state props]
  [:div.card
   [:div.card-header
    [:h2.d-inline.h5.me-2 "Create a custom filter"]
    [:span.text-muted "search for a code"]
    (search-form state props)]
   (when (-> @state :results :codes)
     [:div.card-body
      (filters/filters state props)])])

(defn ui [state props]
  (c/state-wrapper
   state
   [:h1.my-4 "Structural Search"]
   (create-filter-card state props)
   (res/results state props)))
