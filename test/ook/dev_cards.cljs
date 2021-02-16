(ns ook.dev-cards
  (:require [ook.main :as main]
            [devcards.core :as dc]
            [ook.dev-cards.search] ;; individual cards have to be required somewhere
            ))

(defn ^:export init []
  ;; (main/init)
  (dc/start-devcard-ui!))
