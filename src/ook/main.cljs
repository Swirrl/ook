(ns ook.main
  (:require [ook.ui.state :as state]
            [goog.object :as go]
            [reagent.core :as r]
            [cljs.reader :as edn]
            [ook.ui.error-boundary :as err]
            [reagent.dom :as rdom]
            [ook.ui.search :as search]
            [ook.concerns.transit :as t]
            [ook.concerns.router :as router]))

(defn pre-init []
  (if ^boolean goog/DEBUG
    (println "*** starting OOK in dev mode ***")
    (set-print-fn! (constantly nil))))

(def ^:private id->view-fn
  {:search search/ui})

(defn read-state [el]
  (let [encoded-state (some-> el
                              (go/get "attributes")
                              (go/get "data-ook-init")
                              (go/get "value"))]
    (t/read-string encoded-state)))

(defn- hydrate-component [el id]
  (swap! state/components-state assoc id :loading)
  (let [state (read-state el)
        cursor (r/cursor state/components-state [id])
        view-fn (id->view-fn id)]
    (println "Hydrating component " id " from data attribute")
    (swap! state/components-state assoc id state)
    (rdom/render [err/error-boundary
                  [(partial view-fn cursor)]]
                 el)))

(defn- find-components []
  (->> (.getElementsByClassName js/document "OokComponent")
       array-seq))

(defn- mount-components []
  (doseq [el (find-components)
          :let [id (edn/read-string (go/get el "id"))]]
    (hydrate-component el id)))

(defn ^:export init
  "Client side entry point called from the main layout"
  []
  (mount-components)
  (router/init!))
