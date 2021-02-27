(ns ook.reframe.views.filters
  (:require
   [re-frame.core :as rf]
   [clojure.string :as str]
   [ook.util :as u]))

(defn- single-label [label]
  (if (coll? label)
    (str/join ", " label)
    label))

(defn- apply-code-selection [event]
  (.preventDefault event)
  (rf/dispatch [:app/navigate :ook.route/search]))

(defn filters []
  (let [codes @(rf/subscribe [:results.codes/data])
        query @(rf/subscribe [:results.codes/query])
        cnt (count codes)]
    (when codes
      (list
       ^{:key "answer"}
       [:p.m-0 "Found " [:strong cnt (u/pluralize " code" cnt)] " matching " [:strong "\"" query "\""]]
       ^{:key "data"}
       [:form {:id "codes" :on-submit apply-code-selection}
        (when (seq codes)
          [:button.btn.btn-primary.mt-2.mb-4 {:type "submit"} "Apply selection"])
        (doall
         (for [{:keys [id label scheme]} codes
               :let [value (str id "," scheme)]]
           ^{:key id} [:div.form-check.mb-3.bg-light
                       [:div.p-2
                        [:input.form-check-input
                         {:type "checkbox"
                          :name "code"
                          :value value
                          :checked (-> @(rf/subscribe [:ui.codes/selection]) (get value))
                          :id id
                          :on-change #(rf/dispatch [:ui.codes/toggle-selection
                                                    (-> % .-target .-value)])}]
                        [:label.form-check-label {:for id}
                         [:strong (single-label label)]
                         [:p.m-0 "id: " [:code id]]]]]))]))))
