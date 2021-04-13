(ns ook.test.util.event-helpers
  (:require [reagent.core :as r]
            [ook.test.util.query-helpers :as qh]
            ["@testing-library/user-event" :as ue]))

(defn click [target]
  (.click ue/default target)
  (r/flush))

(defn click-all [targets]
  (doseq [t targets]
    (click t)))

(defn select-from-dropdown [dropdown label]
  (let [option (qh/find-text dropdown label)]
    (if option
      (do
        (set! (.. option -selected) true)
        (r/flush))
      (throw (js/Error. (str "No option found with label \"" label "\"."))))))

(defn clear-input [input]
  (.clear ue/default input))

(defn- send-keys [target val]
  (.type ue/default target val)
  (r/flush))

(defn set-input-val [input val]
  (clear-input input)
  (send-keys input val))

(defn press-enter [target]
  (send-keys target "{enter}"))

(defn click-text [text]
  (click (qh/find-text text)))

;;;;;;; OOK-specific UI helpers

(defn cancel-facet-selection []
  (click (qh/cancel-facet-selection-button)))

(defn click-expansion-toggle [label]
  (click (qh/find-expand-toggle label)))

(defn click-select-any [label]
  (click (qh/select-any-button label)))

(defn click-select-toggle [label]
  (click (qh/find-text label)))

(defn click-select-all-children [label]
  (click (qh/all-children-button label)))
