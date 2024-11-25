(ns ook.reframe.codes.view
  (:require
   [re-frame.core :as rf]
   [ook.ui.common :as common]))

(defn nested-list [opts & children]
  [:ul.children opts (common/with-react-keys children)])

(defn nested-list-item [opts & children]
  [:li.child opts
   (common/with-react-keys children)])

(defn checkbox-input [{:keys [ook/uri label used] :as option}]
  (let [checked-state @(rf/subscribe [:ui.facets.current/checked-state option])
        id (str (gensym (str uri "-react-id-")))]
    [:<>
     [:input.form-check-input.me-2
      (cond-> {:type "checkbox"
               :name "code"
               :value uri
               :id id
               :checked (= :checked checked-state)
               :on-change #(rf/dispatch [:ui.event/toggle-selection option])}
        (not used) (assoc :disabled true)
        (= :indeterminate checked-state) (assoc :class "indeterminate-checkbox"))]
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
