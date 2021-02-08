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
          [:html {:lang :en}
           contents]))

(defn- head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
   [:title  "ONS Trade Search"]
   [:script {:src "js/main.js" :defer true :type "text/javascript"}]
   [:script "ook.main.init()"]])

(defn- header []
  [:header "This is the header"])

(defn- footer []
  [:footer "This is the footer"])

(defn- body [contents]
  [:body
   (header)
   [:main  contents]
   (footer)])

(defn- layout [contents]
  (page
   (head)
   (body contents)))

(defn ->html [& contents]
  (-> contents layout h/html str))
