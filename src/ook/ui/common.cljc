(ns ook.ui.common)

(defn loading-spinner []
  [:div.mt-4.spinner-border {:role "status"}
   [:span.visually-hidden "Loading..."]])
