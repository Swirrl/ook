(ns ook.ui.filters
  (:require [ook.ui.common :as c]
            [clojure.string :as str]
            [ook.util :as u]
            #?@(:cljs [[reagent.core :as r]
                       [ook.ui.state :as state]])))

(defn- single-label [label]
  (if (coll? label)
    (str/join ", " label)
    label))

#?(:cljs
   (defn update-code-selection [event]
     (let [state (r/cursor state/components-state [:main :ui :codes :selection])
           checkbox (-> event .-target .-value)]
       (swap! state update checkbox not))))

(defn filters [state {:keys [handler/apply-code-selection]}]
  (let [{:keys [results]} @state
        query (-> results :codes :query)
        data (-> results :codes :data)
        cnt (count data)]
    (when query
      (c/siblings
       [:p "Found " [:strong cnt (u/pluralize " code" cnt)] " matching " [:strong "\"" query "\""]]
       (when (seq data)
         [:form
          #?(:clj {:id "codes" :action "/search" :method "GET"}
             :cljs {:id "codes" :on-submit apply-code-selection})
          [:button.btn.btn-primary.mt-2.mb-4 {:type "submit"} "Apply selection"]
          (doall
           (for [{:keys [id label scheme]} data
                 :let [value (str id "," scheme)]]
             ^{:key id} [:div.form-check.mb-3.bg-light
                         [:div.p-2
                          [:input.form-check-input
                           (-> {:type "checkbox"
                                :name "code"
                                :value value
                                :checked (-> @state :ui :codes :selection (get value))
                                :id id}
                               #?(:cljs (merge {:on-change update-code-selection})))]
                          [:label.form-check-label {:for id}
                           [:strong (single-label label)]
                           [:p.m-0 "id: " [:code id]]]]]))])))))
