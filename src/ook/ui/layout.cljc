(ns ook.ui.layout
  (:require [hiccup2.core :as h]
            [hiccup.util :as h.u]
            #?@(:cljs [[reframe.core :as rf]])
            [ook.concerns.transit :as t]
            [clojure.string :as st]))

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
   [:title  "IDS Structural Search"]
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
  [:div.row
   [:div.d-flex.justify-content-between.pt-5
    [:h1.mb-3
     [:a.link-dark.text-decoration-none #?(:clj {:href "/"}
                      :cljs {:on-click #(rf/dispatch [:app/navigate :ook.route/home])})
      "Structural Search"]]
    [:div [:span.align-top.badge.bg-warning.text-dark "Alpha-Stage Prototype"]]]
   [:p.lead.pb-3 "Search for observations from a range of datasets, using dimensions and classification codes."]])

(defn- footer []
  [:footer.mt-auto.footer.bg-light.p-4
   [:div.container
    [:p.m-0
     "Created by Swirrl and the ONS in collaboration with DIT as part of the "
     [:a.link-dark {:href "https://beta.gss-data.org.uk/"} "Integrated Data Service"]
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

(defn search-form [query]
  [:form.row
   {:action "/" :method "get"}
   [:div.col
    [:input.form-control
     {:id "query"
      :name "query"
      :type "search"
      :aria-label "Search term"
      :value query}]]
   [:div.col
    [:input.btn.btn-primary.mb-3
     {:type "submit"
      :value "Search"
      :aria-label "Submit search"}]]])

(defn search-results [datasets]
  [:div.row
   [:p (str "Found " (count datasets) " results")]
   (for [{:keys [:ook/uri
                 label
                 cube
                 comment
                 snippet]
          :as dataset} datasets]
     [:div.mb-5
      [:div
       [:span.text-secondary cube]
       [:br]
       [:div.d-flex.w-100.justify-content-between
        [:a.text-decoration-none
         {:href uri}
         [:h3 label]]
        ;;[:small "X matching observations"]
        ]]
      [:div
       [:p comment]
       [:div.container
        [:div.row
         (for [{:keys [:ook/uri label codelist]} (:dimensions snippet)]
           (let [ldim (if (and (contains? codelist :label)
                               (not= label (:label codelist)))
                        (str label " (" (:label codelist) ")")
                        label)
                 lvalue (if-let [matches (:matches codelist)]
                          (->> matches
                               (map :label)
                               (st/join " | ")
                               (str ": ")))]
             [:div.col-12
              [:span.text-muted
               [:strong ldim]
               lvalue]]))]]]])])

(defn search-body [{:keys [query datasets]}]
  [:body.d-flex.flex-column.h-100
   [:main.flex-shrink-0
    [:div.mt-3.container
     (header)
     (search-form query)
     (if (not (empty? datasets))
       (search-results datasets))]]
   (footer)])

(defn search [fingerprint-path data]
  (page
   (head fingerprint-path)
   (search-body data)))
