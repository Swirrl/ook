(ns ook.concerns.reitit
  (:require [integrant.core :as ig]
            [reitit.ring :as ring]
            [meta-merge.core :as mm]))

(defmethod ig/init-key :ook.concerns.reitit/ring-handler
  [_ {:keys [router default-handler opts]}]
  (ring/ring-handler router default-handler opts))

(defmethod ig/init-key :ook.concerns.reitit/router [_ {:keys [data opts]}]
(ring/router data (mm/meta-merge {
                                  ;; stuff like coercion, middlewares etc. goes here
                                  }
                                 opts)))
