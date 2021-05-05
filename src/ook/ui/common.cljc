(ns ook.ui.common)

(defn loading-spinner []
  [:div.spinner-border {:role "status"}
   [:span.visually-hidden "Loading..."]])

(defn with-react-keys [children]
  (vec (cons :<>
             (map (fn [v]
                    (with-meta v {:key (gensym "react")}))
                  children))))

(defn error-message [message]
  [:div.alert.alert-danger.mt-3 message])

(defn primary-button [opts content]
  [:button.btn.btn-primary (merge {:type "button"} opts) content])

(defn text-button [opts & children]
  [:button.btn.btn-link.mx-1.p-0.align-baseline
   (merge opts {:type "button"})
   children])
