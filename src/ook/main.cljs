(ns ook.main)

(defn pre-init []
  (if ^boolean goog/DEBUG
    (println "*** starting OOK in dev mode ***")
    (set-print-fn! (constantly nil))))

(defn- mount-components []
  (println "there are no components yet ..."))

(defn ^:export init
  "Client side entry point called via dev.cljs/prod.cljs depending on your env"
  []
  (mount-components))
