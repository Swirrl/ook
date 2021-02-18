(ns ook.etl
  (:require
   [drafter-client.client.impl :as dci]
   [clojure.java.io :as io]
   [grafter-2.rdf4j.io :as gio]
   [grafter-2.rdf.protocols :as gpr])
  (:import (java.io ByteArrayOutputStream)
           (com.github.jsonldjava.utils JsonUtils)
           (com.github.jsonldjava.core JsonLdOptions JsonLdProcessor)))

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
    ([system graphs] (construct system query :named-graph-uri graphs))))

(def extract-datasets
  (constructor (slurp (io/resource "etl/dataset-construct.sparql"))))

(def extract-codes
  (constructor (slurp (io/resource "etl/code-construct.sparql"))))

(def extract-components
  (constructor (slurp (io/resource "etl/component-construct.sparql"))))


(def ^:private ;; once?
  jsonld-options
  (doto (JsonLdOptions.) (.setUseNativeTypes true)))

(defn ->jsonld [statements]
  (with-open [output (ByteArrayOutputStream.)]
    (let [wtr (gio/rdf-writer (io/output-stream output) :format :jsonld)]
      (gpr/add wtr statements))
    (JsonUtils/fromString (str output))))

(defn compact [context input]
  (JsonLdProcessor/compact input context jsonld-options))

(defn frame [frame-doc input]
  (JsonLdProcessor/frame input frame-doc jsonld-options))

(defn transform [frame-file statements]
  (let [frame-doc (JsonUtils/fromString (slurp frame-file))]
    (-> statements
        ->jsonld
        ((partial compact frame-doc))
        ((partial frame frame-doc)))))

(def transform-datasets
  (partial transform (io/resource "etl/dataset-frame.json")))

(def transform-codes
  (partial transform (io/resource "etl/code-frame.json")))

(def transform-components
  (partial transform (io/resource "etl/component-frame.json")))
