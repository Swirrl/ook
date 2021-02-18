(ns ook.dev-cards
  (:require [devcards.core :as dc]
            [ook.dev-cards.search] ;; individual cards have to be required somewhere
            ))

(defn ^:export init []
  (dc/start-devcard-ui!))
