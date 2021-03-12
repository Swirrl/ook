(ns ook.main
  (:require
   [ook.reframe.events]
   [ook.reframe.router :as router]
   [ook.reframe.subs]
   [ook.reframe.views :as views]
   [re-frame.core :as rf]
   [reagent.dom :as rdom]))

(defn pre-init []
  (if ^boolean goog/DEBUG
    (println "*** starting OOK in dev mode ***")
    (set-print-fn! (constantly nil))))

(defn- render []
  (rdom/render [views/main] (.getElementById js/document "app")))

(defn ^:export init []
  (router/init!)
  (render))

(defn ^:dev/after-load clear-cache-and-render!
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code. We force a UI update by clearing
  ;; the Reframe subscription cache.
  (rf/clear-subscription-cache!)
  (render))
