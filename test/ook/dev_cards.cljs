(ns ook.dev-cards
  (:require [ook.main :as main]
            [devcards.core :as dc]))

(defn ^:export init []
  (main/init)
  (dc/start-devcard-ui!))
