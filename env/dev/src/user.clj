(ns user)

(defn help []
  (println "Welcome")
  (println)
  (println "Available commands are:")
  (println)
  (println "(go)       ;; launch the project for the first time")
  (println "(reset)    ;; refresh and reload the project")
  (println "(halt)     ;; shutdown the running system"))


(defn dev
  "Load and switch to the 'dev' namespace."
  []
  (require 'dev)
  (help)
  (in-ns 'dev)
  :loaded)
