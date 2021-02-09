(ns ook.ui.layout
  (:require [hiccup2.core :as h]
            [hiccup.util :as h.u]))

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
   [:nav.navbar.navbar-light.bg-light
    [:div.container-fluid
     [:span.navbar-brand.mb-0.h1 "ONS Trade Search"]]]])

(defn- footer []
  [:footer.footer.mt-auto.bg-light.p-3
   "by Swirrl"])

(defn- scripts []
  (list
   ;; <!-- JavaScript Bundle with Popper -->
   ;; <script src= "https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta1/dist/js/bootstrap.bundle.min.js" integrity= "sha384-ygbV9kiqUc6oa4msXn9868pTtWMgiQaeYH7/t7LECLbyPA2x65Kgf80OJFdroafW" crossorigin= "anonymous" ></script>

   [:script {:src "/assets/js/main.js" :type "text/javascript"}]
   [:script "ook.main.init()"]))

(defn- body [contents]
  [:body.d-flex.flex-column.h-100
   (header)
   [:main.flex-shrink-0.p-3 contents]
   (footer)
   (scripts)])

(defn- layout [contents]
  (page
   (head)
   (body contents)))

(defn ->html [& contents]
  (-> contents layout h/html str))
