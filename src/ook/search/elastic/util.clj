(ns ook.search.elastic.util
  (:require [clojurewerkz.elastisch.rest :as esr]))

(defn get-connection [endpoint]
  (esr/connect endpoint {:content-type :json}))
