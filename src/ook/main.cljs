(ns ook.main
  (:require [ook.ui.state :as state]
            [goog.object :as go]
            [cljs.reader :as edn]
            [ook.ui.error-boundary :as err]
            [ook.ui.search :as search]
            [ook.concerns.transit :as t]
            ;; [ook.concerns.router :as router]
            [ook.handler :as handler]

            ;;

            [re-frame.core :as rf]
            [reagent.dom :as rdom]
            [ook.reframe.views :as views]
            [ook.reframe.router :as router]
            ;; these just need to be included somewhere
            [ook.reframe.events]
            [ook.reframe.subs]))

(defn pre-init []
  (if ^boolean goog/DEBUG
    (println "*** starting OOK in dev mode ***")
    (set-print-fn! (constantly nil))))

;; (def ^:private id->view-fn
;;   {:main search/ui})

;; (def ^:private id->props
;;   {:main {:handler/submit-search handler/submit-search
;;           :handler/apply-code-selection handler/apply-code-selection}})

;; (defn read-state [el]
;;   (let [encoded-state (some-> el
;;                               (go/get "attributes")
;;                               (go/get "data-ook-init")
;;                               (go/get "value"))]
;;     (t/read-string encoded-state)))

;; (defn- hydrate-component [el id]
;;   (swap! state/components-state assoc id :loading)
;;   (let [state (read-state el)
;;         cursor (r/cursor state/components-state [id])
;;         view-fn (id->view-fn id)
;;         props (id->props id)]
;;     (println "Hydrating component " id " from data attribute")
;;     (swap! state/components-state assoc id state)
;;     (rdom/render [err/error-boundary
;;                   [view-fn cursor props]]
;;                  el)))

;; (defn- find-components []
;;   (->> (.getElementsByClassName js/document "OokComponent")
;;        array-seq))

;; (defn- mount-components []
;;   (doseq [el (find-components)
;;           :let [id (edn/read-string (go/get el "id"))]]
;;     (hydrate-component el id)))

;; (defn ^:export init
;;   "Client side entry point called from the main layout"
;;   []
;;   (mount-components)
;;   (router/init!))

;; OLD STUFF ☝️
;;;;;;;;;;;;;;;;;;

;; NEW REFRAME STUFF


(defn- render []
  (rdom/render [views/main] (.getElementById js/document "app")))

(defn ^:dev/after-load clear-cache-and-render!
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code. We force a UI update by clearing
  ;; the Reframe subscription cache.
  (rf/clear-subscription-cache!)
  (render))

(defn ^:export init []
  (rf/dispatch-sync [:init/initialize-db])
  (router/init!)
  (render))
