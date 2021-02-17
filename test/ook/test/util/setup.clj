(ns ook.test.util.setup
  (:require [integrant.core :as ig]
            [clojure.java.io :as io]
            [ook.main :as main]))

(def test-profiles (concat main/core-profiles [(io/resource "test.edn")]))

(defn start-test-system! []
  (main/exec-config {:profiles (map main/load-config test-profiles)}))

(def stop-system! ig/halt!)

(defmacro with-test-system
  "Start a test system that uses ook.search.fake as a backend (configured in test.edn)"
  [sym & body]
  `(let [~sym (start-test-system!)]
     (try
       ~@body
       (finally
         (stop-system! ~sym)))))
