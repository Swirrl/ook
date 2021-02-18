(ns ook.concerns.integrant
  "This namespace defines various integrant concerns, e.g.
    data-readers and derived constant keys etc.

  This file should be explicitly required by entry points to the
  app."
  (:require [integrant.core :as ig]))

(defmethod ig/init-key :ook/const [_ v] v)

(derive :ook.concerns.elastic/endpoint :ook/const)

(defn env [[env-var default]]
  (or (System/getenv env-var) default))
