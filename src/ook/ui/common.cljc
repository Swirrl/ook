(ns ook.ui.common)

(defn loading-spinner []
  [:div.spinner-border {:role "status"}
   [:span.visually-hidden "Loading..."]])

(defn with-react-keys [children]
  (vec (cons :<>
             (map (fn [v]
                    (with-meta v {:key (gensym "react")}))
                  children))))
