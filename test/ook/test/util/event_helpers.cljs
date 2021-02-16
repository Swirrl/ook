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

(defn set-input-val [input val]
  (.type ue/default input val)
  (r/flush))
