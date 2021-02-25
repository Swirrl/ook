(ns ook.etl
  (:require
   [drafter-client.client.impl :as dci]
   [drafter-client.client.interceptors :as interceptors]
   [clojure.java.io :as io]
   [grafter-2.rdf4j.io :as gio]
   [grafter-2.rdf.protocols :as gpr]
   [clojurewerkz.elastisch.rest :as es]
   [clojurewerkz.elastisch.rest.bulk :as esb]
   [clojure.data.json :as json]
   [clojure.tools.logging :as log]
   [clojure.string :as s])
  (:import (java.io ByteArrayOutputStream)
           (com.github.jsonldjava.utils JsonUtils)
           (com.github.jsonldjava.core JsonLdOptions JsonLdProcessor)))

(defn query
  "Gets a query from the live draftset.
   May include `:named-graph-uri` or `:default-graph-uri` options."
  ([client query & opts]
   (let [args (concat [client query] opts);
         response (apply dci/post-query-live args)]
     (:body response))))

(defn insert-values-clause
  "Adds a VALUES clause to a sparql query string with the URIs provided"
  [query-string var-name uris]
  {:pre [(string? query-string) (string? var-name) (coll? uris)]}
  (let [[top bottom] (s/split query-string #"(?<=WHERE \{)")
        terms (s/join " " (map #(str "<" % ">") uris))
        clause (str "\n  VALUES ?" var-name " { " terms " }\n")]
    (str top clause bottom)))

;; Select subjects for paging
(defn row-seq
  "Returns a lazy seq of lines from the reader, closing it when everything is consumed"
  [^java.io.BufferedReader rdr]
  (if-let [row (.readLine rdr)]
    (cons row (lazy-seq (row-seq rdr)))
    (.close rdr)))

(def page-length 50000)

(defn subject-pages
  "Executes a query to get a collection of URIs. Returns pages of URIs for
   inserting into another query. Each page is a map of the var name to a seq of URIs.
   NB: Only expecting a single variable to be bound in the results.
   See `insert-values-clause`."
  [{:keys [drafter-client/client] :as system} page-query]
  (log/info "Fetching subjects")
  (let [client (interceptors/accept client "text/csv")
        reader (io/reader (query client page-query))]
    (let [[name & values] (row-seq reader)]
      (map #(assoc {} name %)
           (partition-all page-length values)))))

(defn extract
  "Executes the construct query binding in values from page"
  [{:keys [drafter-client/client] :as system} construct-query [[var-name uris] & _]]
  (log/info "Constructing resources")
  (let [query-string (insert-values-clause construct-query var-name uris)]
    (query client query-string)))



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

(defn transform [frame-string statements]
  (log/info "Transforming objects")
  (let [frame-doc (JsonUtils/fromString frame-string)]
    (-> statements
        ->jsonld
        ((partial compact frame-doc))
        ((partial frame frame-doc)))))



;; Load

(defn add-id [object]
  (assoc (into {} object) :_id (get object "@id")))

(defn load-documents [{:keys [:ook.concerns.elastic/endpoint] :as system} index jsonld]
  (log/info "Loading documents")
  (let [conn (es/connect endpoint {:content-type :json})
        operations (esb/bulk-index (map add-id (get jsonld "@graph")))]
    (esb/bulk-with-index conn index operations)))

#_(def batch-size 40000)

#_(defn load-documents [{:keys [:ook.concerns.elastic/endpoint] :as system} index jsonld]
  (log/info "Loading documents")
  (let [conn (es/connect endpoint {:content-type :json})
        doc-batches (partition-all batch-size (map add-id (get jsonld "@graph")))]
    (doseq [docs doc-batches]
      (esb/bulk-with-index conn index (esb/bulk-index docs)))))


;; Pipeline

(defn pipeline-fn [page-query construct-query jsonld-frame index]
  (fn [system]
    (log/info (str "Pipeline Started:" index))
    (doseq [page (subject-pages system page-query)]
      (log/info "Processing page")
      (->> (extract system construct-query page)
           (transform jsonld-frame)
           (load-documents system index)))
    (log/info (str "Pipeline Complete: " index))))

(def dataset-pipeline
  (pipeline-fn
   (slurp (io/resource "etl/dataset-select.sparql"))
   (slurp (io/resource "etl/dataset-construct.sparql"))
   (slurp (io/resource "etl/dataset-frame.json"))
   "dataset"))

(def component-pipeline
  (pipeline-fn
   (slurp (io/resource "etl/component-select.sparql"))
   (slurp (io/resource "etl/component-construct.sparql"))
   (slurp (io/resource "etl/component-frame.json"))
   "component"))

(def code-pipeline
  (pipeline-fn
   (slurp (io/resource "etl/code-select.sparql"))
   (slurp (io/resource "etl/code-construct.sparql"))
   (slurp (io/resource "etl/code-frame.json"))
   "code"))

(def observation-pipeline
  (pipeline-fn
   (slurp (io/resource "etl/observation-select.sparql"))
   (slurp (io/resource "etl/observation-construct.sparql"))
   (slurp (io/resource "etl/observation-frame.json"))
   "observation"))

(defn pipeline [system]
  (log/info "Running all pipelines")
  (dataset-pipeline system)
  (component-pipeline system)
  (code-pipeline system)
  (observation-pipeline system)
  (log/info "All pipelines complete"))
