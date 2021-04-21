(ns ook.main
  (:require
   [ook.reframe.events]
   [ook.reframe.events.filter-ui]
   [ook.reframe.events.codes]
   [ook.reframe.events.facets]
   [ook.reframe.router :as router]
   [ook.reframe.subs]
   [ook.reframe.views :as views]
   [ook.concerns.transit :as t]
   [goog.object :as go]
   [re-frame.core :as rf]
   [reagent.dom :as rdom]))

(defn pre-init []
  (if ^boolean goog/DEBUG
    (println "*** starting OOK in dev mode ***")
    (set-print-fn! (constantly nil))))

(defn- render [el]
  (rdom/render [views/main] el))

(defn ^:export init []
  (router/init!)
  (let [el (.getElementById js/document "app")
        initial-state (-> el
                          (go/get "attributes")
                          (go/get "data-init")
                          (go/get "value")
                          (t/read-string))]
    (rf/dispatch-sync [:init/initialize-db initial-state])
    (render el)))

(defn ^:dev/after-load clear-cache-and-render!
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code. We force a UI update by clearing
  ;; the Reframe subscription cache.
  (rf/clear-subscription-cache!)
  (let [el (.getElementById js/document "app")]
    (render el)))
