(ns ook.etl
  (:require
   [drafter-client.client.impl :as dci]
   [clojure.java.io :as io]))

(defn construct
  "Gets a construct query from the live draftset"
  ([{:keys [drafter-client/client] :as system} query & opts]
   (let [args (concat [client query] opts)
         response (apply dci/get-query-live args)]
     (:body response))))

(defn constructor [query]
  "Creates a function for calling construct queries with optional graph restrictions"
  (fn
    ([system] (construct system query))
    ([system graphs] (construct system query :default-graph-uri graphs))))

(def extract-datasets
  (constructor (slurp (io/resource "etl/dataset-construct.sparql"))))

(def extract-codes
  (constructor (slurp (io/resource "etl/code-construct.sparql"))))

(def extract-components
  (constructor (slurp (io/resource "etl/component-construct.sparql"))))



