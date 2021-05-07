(ns ook.reframe.codes.view
  (:require
   [re-frame.core :as rf]
   [ook.ui.common :as common]))

(defn nested-list [opts & children]
  [:ul.list-group-flush opts (common/with-react-keys children)])

(defn nested-list-item [opts & children]
  [:li.list-group-item.border-0.pb-0 opts (common/with-react-keys children)])

(defn top-tree-level [& children]
  [:form.mt-3
   [nested-list {:class "p-0"}
    children]])

(defn checkbox-input [{:keys [ook/uri label used] :as option}]
  (let [selected? @(rf/subscribe [:ui.facets.current/option-selected? option])
        id (str (gensym (str uri "-react-id-")))]
    [:<>
     [:input.form-check-input.mx-2
      (cond-> {:type "checkbox"
               :name "code"
               :value uri
               :id id
               :checked selected?
               :on-change #(rf/dispatch [:ui.event/toggle-selection option])}
        (not used) (merge {:disabled true}))]
     [:label.form-check-label.d-inline {:for id} label]]))

(defn codelist-wrapper [codelist-uri code-tree]
  (let [status @(rf/subscribe [:ui.codes/status codelist-uri])]
    (condp = status
      :loading
      [nested-list [nested-list-item (common/loading-spinner)]]

      :error
      [nested-list
       [nested-list-item
        [common/error-message "Sorry, there was an error fetching the codes for this codelist."]]]

      :ready code-tree

      [:div])))
