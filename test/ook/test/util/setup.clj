(ns ook.test.util.setup
  (:require [clojure.java.io :as io]
            [ook.main :as main]))

(def test-profiles (concat main/core-profiles [(io/resource "test.edn")]))
