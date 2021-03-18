(ns ook.search.elastic-test
  (:require [clojure.test :refer [deftest testing is]]
            [ook.concerns.integrant :refer [with-system]]
            [ook.etl :as etl]
            [clojure.java.io :as io]
            [vcr-clj.clj-http :refer [with-cassette]]
            [ook.search.elastic :as es]
            [ook.search.db :as sut]))

(def db (es/->Elasticsearch {:elastic/endpoint "http://localhost:9201"}))

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

(deftest database-test
  (testing "Extracting a page of RDF from a drafter endpoint"
    (with-system [system ["drafter-client.edn", "cogs-staging.edn" "elasticsearch-test.edn"]]
      (load-datasets! system)

      (testing "all datasets"
        (let [response (sut/all-datasets db)]
          (is (= 3 (count response)))
          (is (every? true? (map #(every? % [:comment :label :id :cube]) response)))
          (is (= ["Alcohol Bulletin - Clearances"
                  "Alcohol Bulletin - Duty Receipts"
                  "Alcohol Bulletin - Production"]
                 (map :label response))))))))
