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
  (:import (java.io File ByteArrayOutputStream)
           (com.github.jsonldjava.utils JsonUtils)
           (com.github.jsonldjava.core JsonLdOptions JsonLdProcessor)))


;; Pipeline debugging

(defn write-to-disk
  "Pipeline function to print sequence values to disk (returning sequence for further processing)"
  ([s]
   (write-to-disk s (File/createTempFile "ook-etl-" ".tmp" (new File "/tmp"))))
  ([s file]
   (log/info "Writing to:" (.getAbsolutePath file))
   (with-open [w (io/writer file)]
     (if (seq? s)
       (doseq [x s]
         (.write w (prn-str x)))
       (.write w (prn-str s))))
   s))

(defn wait
  "Sleep to avoid overloading stardog with consecutive queries"
  [& _]
  (log/info "Sleeping for 15s")
  (Thread/sleep 15000)
  (log/info "Waking"))


;; Error recovery

(defmacro with-retry
  "If an exception is raised by the expression, it is retried once after waiting 30s"
  [expr]
  `(try
     ~expr
     (catch Exception e#
       (log/warn "Caught Exception: " (.toString e#))
       (wait)
       (log/info "Retrying")
       ~expr)))


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

(defn spill-to-disk
  "Writes data to file"
  [data file]
  (with-open [os (io/output-stream file)
              is (io/input-stream data)]
    (io/copy is os)))

(defn read-paged
  "Reads lines out of file in lazy-seq of pages.
  Pages are vectors - the variable name followed by the values."
  [file page-size]
  (let [rdr (io/reader file)
        var-name (.readLine rdr)
        read-lines (fn this [r]
                     (lazy-seq
                      (if-let [line (.readLine r)]
                        (cons line (this r))
                        (.close r))))]
    (->> (read-lines rdr)
         (partition-all page-size)
         (map (fn [page] (cons var-name page))))))

(defn select-paged
  "Executes a select query returning results in a lazy seq of pages.
  Pages are vectors - the variable name followed by the values.
  Query results are spilled to disk and read out in page-size partitions.
  If a graph query is provided the subject-query is first paged by graph,
  one-at-a-time."
  ([client subject-query page-size]
   (let [subject-cache (File/createTempFile "ook-etl-subject-cache-" ".tmp")]
     (try
       (let [client (interceptors/accept client "text/csv")]
         (spill-to-disk (query client subject-query) subject-cache))
       (read-paged subject-cache page-size)
       (finally (.delete subject-cache)))))
  ([client graph-query subject-query page-size]
   (let [graph-cache (File/createTempFile "ook-etl-graph-cache-" ".tmp")]
     (try
       (let [client (interceptors/accept client "text/csv")]
         (spill-to-disk (query client graph-query) graph-cache))
       (mapcat (fn [[var-name values]]
                 (let [subject-query (insert-values-clause subject-query var-name [values])]
                   (select-paged client subject-query page-size)))
               (read-paged graph-cache 1)) ;; read multi-graph datasets one graph
       (finally (.delete graph-cache))))))



;; Extract
(defn with-dataset-scope [query datasets]
  "Scopes query to target datasets if present"
  (if datasets
    (insert-values-clause query "dataset" datasets)
    query))

(defn subject-pages
  "Executes a query to get a collection of URIs. Returns pages of URIs for
   inserting into another query. Each page is a vector beginning with the var name
   followed by the URIs."
  ([{:keys [drafter-client/client
            ook.etl/target-datasets
            ook.etl/select-page-size]
     :as system
     :or {select-page-size 50000}} subject-query]
   (select-paged client
                 (with-dataset-scope subject-query target-datasets)
                 select-page-size))
  ([{:keys [drafter-client/client
            ook.etl/target-datasets
            ook.etl/select-page-size]
     :as system
     :or {select-page-size 50000}} graph-query subject-query]
   (select-paged client
                 (with-dataset-scope graph-query target-datasets)
                 subject-query
                 select-page-size)))

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
      (doall (gpr/add wtr statements)))
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

(defn- first-error [result]
  (->> result
       :items
       (map (comp :error first vals))
       (remove nil?)
       first))

(defn bulk-upsert
  "Generates operations for a bulk-upsert (esb/bulk-update doesn't quite work)"
  [docs]
  (let [ops (map (fn [doc] {"update" {"_id" (get doc "@id")}}) docs)
        docs (map (fn [doc] {"doc" (dissoc doc :_id), "doc_as_upsert" true}) docs)]
    (interleave ops docs)))

(defn load-documents [{:keys [:ook.concerns.elastic/endpoint
                              :ook.etl/load-page-size
                              :ook.etl/load-synchronously] :as system} index jsonld]
  (log/info "Loading documents into" index "index")
  (let [conn (es/connect endpoint {:content-type :json})
        docs (map add-id (get jsonld "@graph"))
        batches (partition-all (or load-page-size 10000) docs)
        params (if load-synchronously {:refresh "wait_for"} {})]
    (doall
     (for [batch batches]
       (let [result (esb/bulk-with-index conn index (bulk-upsert batch) params)]
         (if (:errors result)
           (throw (ex-info "Error loading documents" (first-error result))))
         result)))))

(derive :ook.etl/load-synchronously :ook/const)





;; Pipeline

(defn pipeline-fn* [pager-fn construct-query jsonld-frame index]
  (fn [system]
    (log/info (str "Pipeline Started: " index))
    (let [counter (atom 0)]
      (doseq [[var-name & uris] (pager-fn system)]
        (log/info "Processing page starting with" index "subject" @counter)
        (if uris
          (do
            (swap! counter + (count uris))
            (with-retry
              (doall
               (->> (extract system construct-query var-name uris)
                    (transform jsonld-frame)
                    (load-documents system index)))))
          (log/warn (str "No compatible (" index ") subjects found!")))))
    (log/info (str "Pipeline Complete: " index))))

(defn pipeline-fn
  ([subject-query construct-query jsonld-frame index]
   (pipeline-fn*
    (fn [system] (subject-pages system subject-query))
    construct-query jsonld-frame index))
  ([graph-query subject-query construct-query jsonld-frame index]
   (pipeline-fn*
    (fn [system] (subject-pages system graph-query subject-query))
    construct-query jsonld-frame index)))

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

(def code-used-pipeline
  (pipeline-fn
   (slurp (io/resource "etl/code-select.sparql"))
   (slurp (io/resource "etl/code-used-construct.sparql"))
   (slurp (io/resource "etl/code-used-frame.json"))
   "code"))

(def observation-pipeline
  (pipeline-fn
   (slurp (io/resource "etl/observation-graph.sparql"))
   (slurp (io/resource "etl/observation-select.sparql"))
   (slurp (io/resource "etl/observation-construct.sparql"))
   (slurp (io/resource "etl/observation-frame.json"))
   "observation"))

(defn pipeline [system]
  (log/info "Running all pipelines")
  (dataset-pipeline system)
  (component-pipeline system)
  (code-pipeline system)
  (let [system (assoc system :ook.etl/select-page-size 200)]
    (code-used-pipeline system))
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
                 "idp-beta.edn"
                 "elasticsearch-development.edn"
                 "load-data.edn"
                 "project/trade/data.edn"
                 ]}))

  ;; update single index
  (require 'ook.index)
  (dev/with-system [system
                    ["drafter-client.edn"
                     "idp-beta.edn"
                     "elasticsearch-development.edn"
                     "project/trade/data.edn"]]
    ;(ook.index/delete-index system "code")
    ;(ook.index/create-index system "code")
    #_(let [system (assoc system :ook.etl/select-page-size 50000)]
      (code-pipeline system))
    (let [system (assoc system :ook.etl/select-page-size 200)]
      (code-used-pipeline system)))

  (dev/with-system [system
                    ["drafter-client.edn"
                     "idp-beta.edn"
                     "elasticsearch-development.edn"
                     "project/trade/data.edn"]]

    (let [system (assoc system :ook.etl/select-page-size 10)]
      (component-pipeline system)))

  ;; recreate mid-pipeline error
  (dev/with-system [system
                    ["drafter-client.edn"
                     "idp-beta.edn"
                     "elasticsearch-development.edn"
                     "project/trade/data.edn"]]
    (let [subject-query (slurp (io/resource "etl/observation-select.sparql"))
          target-datasets (:ook.etl/target-datasets system)
          client (:drafter-client/client system)
          subject-query (insert-values-clause subject-query "dataset" target-datasets)
          [var-name & uris] (first (select-paged client subject-query 50000 6250000))
          construct-query (slurp (io/resource "etl/observation-construct.sparql"))
          jsonld-frame (slurp (io/resource "etl/observation-frame.json"))
          index "observation"]
      (if uris
        (->> (extract system construct-query var-name uris)
             (transform jsonld-frame)
             ;;(load-documents system index)
             )
        (log/warn (str "No compatible (" index ") subjects found!"))))))
