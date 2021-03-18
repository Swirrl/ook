(ns ook.test.util.setup
  (:require [clojure.java.io :as io]
            [ook.etl :as etl]
            [integrant.core :as ig]
            [ook.concerns.integrant :as i]
            [vcr-clj.clj-http :refer [with-cassette]]
            [ook.main :as main]))

(def test-profiles (concat main/core-profiles [(io/resource "test.edn")]))

(defn start-system! [profiles]
  (i/exec-config {:profiles profiles}))

(def stop-system! ig/halt!)

(defmacro with-system
  "Start a system with the given profiles"
  [[sym profiles] & body]
  `(let [~sym (start-system! ~profiles)]
     (try
       ~@body
       (finally
         (when (:ook.concerns.elastic/endpoint ~sym)
           (ook.index/delete-indicies ~sym))
         (stop-system! ~sym)))))

(def example-cubes
  ["http://gss-data.org.uk/data/gss_data/trade/HMRC-alcohol-bulletin/alcohol-bulletin-production#dataset"
   "http://gss-data.org.uk/data/gss_data/trade/HMRC-alcohol-bulletin/alcohol-bulletin-duty-receipts#dataset"
   "http://gss-data.org.uk/data/gss_data/trade/HMRC-alcohol-bulletin/alcohol-bulletin-clearances#dataset"])

(defn example-datasets [system]
  (with-cassette :extract-datasets
    (let [query (slurp (io/resource "etl/dataset-construct.sparql"))]
      (etl/extract system query "qb" example-cubes))))

(defn load-datasets! [system]
  (let [datasets (example-datasets system)
        frame (slurp (io/resource "etl/dataset-frame.json"))
        jsonld (etl/transform frame datasets)]
    (etl/load-documents system "dataset" jsonld)))
