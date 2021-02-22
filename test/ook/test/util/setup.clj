(ns ook.test.util.setup
  (:require [integrant.core :as ig]
            [ook.concerns.integrant :as i]
            [clojure.java.io :as io]
            [ook.main :as main]))

(def test-profiles (concat main/core-profiles [(io/resource "test.edn")]))

(def start-test-system! (partial i/start-system! test-profiles))

(def stop-system! i/stop-system!)

(defmacro with-test-system
  "Start a test system that uses ook.search.fake as a backend (configured in test.edn)"
  [sym & body]
  `(let [~sym (start-test-system!)]
     (try
       ~@body
       (finally
         (stop-system! ~sym)))))
