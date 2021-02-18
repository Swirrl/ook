(ns ook.ui.state
  (:require [reagent.core :as r]))

(defonce components-state (r/atom {}))

(defonce match (r/atom nil))
