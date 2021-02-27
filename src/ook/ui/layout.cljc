(ns ook.ui.layout
  (:require [hiccup2.core :as h]
            [hiccup.util :as h.u]
            #?@(:cljs [[reitit.frontend.easy :as rfe]])))

;; Hiccup2 doesn't include versions of the hiccup.page/html5 macro & doesn't
;; work with them. The latter issue seems more of an oversight.
;;
;; See: https://github.com/weavejester/hiccup/issues/144
(defn- page [& contents]
  (h/html {:mode :html}
          (h.u/raw-string "<!doctype html>\n")
          [:html.h-100 {:lang :en}
           contents]))

(defn- head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
   [:title  "ONS Trade Search"]
   [:link {:href "https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta1/dist/css/bootstrap.min.css"
           :rel "stylesheet"
           :integrity "sha384-giJF6kkoqNQ00vy+HMDP7azOuL0xtbfIcaT9wjKHr8RbDVddVHyTfAAsrekwKmP1"
           :crossorigin "anonymous"}]])

(defn- header []
  [:header
   [:nav.navbar.navbar-light.bg-light.mb-3
    [:div.container
     [:span.navbar-brand.mb-0.h1
      [:a #?(:clj {:href "/"}
             :cljs {:href (rfe/href :ook.route/home)})
       "ONS Trade Search"]]]]])

(defn- footer []
  [:footer.footer.bg-light.p-3.mt-auto
   [:div.container "by Swirrl"]])

(defn- scripts []
  (list
   [:script {:src "/assets/js/main.js" :type "text/javascript"}]
   [:script "ook.main.init()"]))

(defn- body []
  [:body.d-flex.flex-column.h-100
   (header)
   [:main.flex-shrink-0.mb-4
    [:noscript "For full functionality of this site it is necessary to enable JavaScript.
 Here are the " [:a {:href "https://enable-javascript.com/"} "instructions for how to enable JavaScript in your web browser."]]
    [:div.container
     [:h1.my-4 "Structural Search"]
     [:div {:id "app"}
      [:p "Loading..."]]]]
   (footer)
   (scripts)])

(defn main []
  (page
   (head)
   (body)))

(defn ->html [contents]
  (-> contents h/html str))

;; #?(:clj (defn main
;;           ([]
;;            (main nil))
;;           ([state]
;;            [:div (cond-> {:id ":main" :class "OokComponent"}
;;                    state (merge {:data-ook-init (t/write-string state)}))
;;             (search/ui (delay state) {})])))
