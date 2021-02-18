(ns ook.etl
  (:require
   [drafter-client.client.impl :as dci]
   [clojure.java.io :as io]
   [grafter-2.rdf4j.io :as gio]
   [grafter-2.rdf.protocols :as gpr]
   [clojurewerkz.elastisch.rest :as es]
   [clojurewerkz.elastisch.rest.index :as esi]
   [clojurewerkz.elastisch.rest.bulk :as esb]
   [clojure.data.json :as json])
  (:import (java.io ByteArrayOutputStream)
           (com.github.jsonldjava.utils JsonUtils)
           (com.github.jsonldjava.core JsonLdOptions JsonLdProcessor)))


;; Extract

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


;; Translate

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


;; Load

(defn create-index [{:keys [:ook.concerns.elastic/endpoint] :as system} index mapping-file]
  (let [conn (es/connect endpoint {:content-type :json})]
    (esi/create conn index (-> mapping-file io/reader json/read))))

(defn create-indicies [system]
  {:dataset (create-index system "dataset" (io/resource "etl/dataset-mapping.json"))
   :component (create-index system "component" (io/resource "etl/component-mapping.json"))
   :code (create-index system "code" (io/resource "etl/code-mapping.json"))
   :observation (create-index system "observation" (io/resource "etl/observation-mapping.json"))})

(defn add-id [object]
  (assoc (into {} object) :_id (get object "@id")))

(defn load-documents [{:keys [:ook.concerns.elastic/endpoint] :as system} index jsonld]
  (let [docs (map (partial into {}) (get jsonld "@graph"))
        conn (es/connect endpoint {:content-type :json})
        operations (esb/bulk-index (map add-id docs))]
    (esb/bulk-with-index conn index operations)))

(defn load-datasets [system jsonld]
  (load-documents system "dataset" jsonld))

(defn load-codes [system jsonld]
  (load-documents system "code" jsonld))

(defn load-components [system jsonld]
  (load-documents system "component" jsonld))
