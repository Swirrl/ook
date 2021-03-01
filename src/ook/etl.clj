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
   [clojure.string :as s]
   [integrant.core :as ig])
  (:import (java.io ByteArrayOutputStream)
           (com.github.jsonldjava.utils JsonUtils)
           (com.github.jsonldjava.core JsonLdOptions JsonLdProcessor)))

;; Query utilities

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

(defn append-limit-offset
  "Adds a LIMIT and OFFSET clause to a sparql query"
  [query-string limit offset]
  {:pre [(string? query-string) (int? limit) (int? offset)]}
  (str query-string "\nLIMIT " limit " OFFSET " offset))

(def page-length 50000)

(defn select-paged
  "Executes a select query one page at a time returning a lazy seq of pages.
   Pages are vectors - the variable name followed by the values."
  ([client query-string]
   (select-paged client query-string page-length))
  ([client query-string page-size]
   (select-paged client query-string page-size 0))
  ([client query-string page-size offset]
   (let [client (interceptors/accept client "text/csv")
         query-page (append-limit-offset query-string page-size offset)
         results (s/split-lines (slurp (io/reader (query client query-page))))]
     (if (< (dec (count results)) page-size)
       (list results)
       (cons results
             (lazy-seq (select-paged client
                                     query-string
                                     page-size
                                     (+ offset page-size))))))))



;; Extract

(defn subject-pages
  "Executes a query to get a collection of URIs. Returns pages of URIs for
   inserting into another query. Each page is a vector beginning with the var name
   followed by the URIs. NB: Only expecting a single variable to be bound in the results.
   See `insert-values-clause`."
  [{:keys [drafter-client/client ook.etl/target-datasets] :as system} subject-query]
  (log/info "Fetching subjects")
  (let [subject-query (if target-datasets
                        (insert-values-clause subject-query "dataset" target-datasets)
                        subject-query)]
    (select-paged client subject-query)))

(defn extract
  "Executes the construct query binding in values from page"
  [{:keys [drafter-client/client] :as system} construct-query var-name uris]
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

(def batch-size 10000)

(defn load-documents [{:keys [:ook.concerns.elastic/endpoint] :as system} index jsonld]
  (log/info "Loading documents into " index " index")
  (let [conn (es/connect endpoint {:content-type :json})
        docs (map add-id (get jsonld "@graph"))
        batches (partition-all batch-size docs)]
    (for [batch batches]
      (esb/bulk-with-index conn index (esb/bulk-index batch)))))


;; Pipeline

(defn pipeline-fn [page-query construct-query jsonld-frame index]
  (fn [system]
    (log/info (str "Pipeline Started:" index))
    (doseq [[var-name & uris] (subject-pages system page-query)]
      (log/info "Processing page")
      (if uris
        (->> (extract system construct-query var-name uris)
             (transform jsonld-frame)
             (load-documents system index))
        (log/warn (str "No compatible (" index ") subjects found!"))))
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


(defmethod ig/init-key ::target-datasets [_ {:keys [sparql client] :as opts}]
  (if sparql
    (let [client (interceptors/accept client "text/csv")
          results (s/split-lines (slurp (io/reader (query client sparql))))]
      (rest results))
    opts))

(comment
  (require 'ook.concerns.integrant)
  (def result
    (ook.concerns.integrant/exec-config
     {:profiles ["drafter-client.edn"
                 "cogs-staging.edn"
                 "elasticsearch-development.edn"
                 ;;"all-data.edn"
                 "trade-data.edn"
                 ]}))

  )
