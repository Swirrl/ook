(ns ook.ui.layout
  (:require [hiccup2.core :as h]
            [hiccup.util :as h.u]
            #?@(:cljs [[reframe.core :as rf]])
            [ook.concerns.transit :as t]))

;; Hiccup2 doesn't include versions of the hiccup.page/html5 macro & doesn't
;; work with them. The latter issue seems more of an oversight.
;;
;; See: https://github.com/weavejester/hiccup/issues/144
(defn- page [& contents]
  (h/html {:mode :html}
          (h.u/raw-string "<!doctype html>\n")
          [:html.h-100 {:lang :en}
           contents]))

(defn- head [fingerprint-path]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
   [:title  "ONS Trade Search"]
   [:link {:href "https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta1/dist/css/bootstrap.min.css"
           :rel "stylesheet"
           :integrity "sha384-giJF6kkoqNQ00vy+HMDP7azOuL0xtbfIcaT9wjKHr8RbDVddVHyTfAAsrekwKmP1"
           :crossorigin "anonymous"}]
   [:link {:href (fingerprint-path "/assets/css/styles.css") :rel "stylesheet" :type "text/css"}]])

(defn error-page [status message fingerprinter]
  (page
   (head fingerprinter)
   [:body.mt-5
    [:main
     [:div.container
      [:h1 status]
      [:p.lead "Sorry, something went wrong."]
      [:p message]]]]))

(defn- header []
  [:div
   [:div.d-flex.justify-content-between.align-items-center.pt-5
    [:h1.mb-3
     [:a.link-dark #?(:clj {:href "/"}
            :cljs {:on-click #(rf/dispatch [:app/navigate :ook.route/home])})
      "Find trade data"]]
    [:div [:span.align-top.badge.bg-warning.text-dark "Alpha-Stage Prototype"]]]
   [:p.lead.pb-3 "Search for observations from a range of datasets, using dimensions and classification codes."]])

(defn- footer []
  [:footer.footer.bg-light.p-3.mt-auto
   [:div.container
    [:p
     "Created by Swirrl and the ONS in collaboration with DIT as part of the "
     [:a {:href "https://beta.gss-data.org.uk/"} "Integrated Data Programme"]
     "."]]])

(defn- scripts [fingerprint-path]
  (list
   [:script {:src (fingerprint-path "/assets/js/main.js") :type "text/javascript"}]
   [:script "ook.main.init()"]))

(defn- body [facets]
  [:body.d-flex.flex-column.h-100
   [:main.flex-shrink-0
    [:div.mt-3.container
     (header)
     [:noscript "For full functionality of this site it is necessary to enable JavaScript.
 Here are the " [:a {:href "https://enable-javascript.com/"} "instructions for how to enable JavaScript in your web browser."]]

     [:div {:id "app" :data-init (t/write-string facets)}]]]
   (footer)])

(defn main [fingerprint-path facets]
  (page
   (head fingerprint-path)
   (body facets)
   (scripts fingerprint-path)))

(defn ->html [contents]
  (-> contents h/html str))
