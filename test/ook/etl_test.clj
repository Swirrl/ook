(ns ook.etl-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [ook.concerns.integrant :as i]
            [ook.index :as idx]
            [clj-http.client :as client]
            [vcr-clj.clj-http :refer [with-cassette]]
            [ook.etl :as sut]))

(def example-cubes
  ["http://gss-data.org.uk/data/gss_data/trade/HMRC-alcohol-bulletin/alcohol-bulletin-production#dataset"
   "http://gss-data.org.uk/data/gss_data/trade/HMRC-alcohol-bulletin/alcohol-bulletin-duty-receipts#dataset"
   "http://gss-data.org.uk/data/gss_data/trade/HMRC-alcohol-bulletin/alcohol-bulletin-clearances#dataset"])

(defn example-datasets [system]
  (with-cassette :extract-datasets
    (let [page {"qb" example-cubes}
          query (slurp (io/resource "etl/dataset-construct.sparql"))]
      (sut/extract system query page))))

(deftest extract-test
  (testing "Extracting a page of RDF from a drafter endpoint"
    (i/with-system [system ["drafter-client.edn", "cogs-staging.edn"]]
      (is (= 33 (count (example-datasets system)))))))

(deftest transform-test
  (testing "Transform triples into json-ld"
    (i/with-system [system ["drafter-client.edn", "cogs-staging.edn"]]
      (let [datasets (example-datasets system)
            frame (slurp (io/resource "etl/dataset-frame.json"))
            jsonld (sut/transform frame datasets)]
        (is (= "Alcohol Bulletin - Clearances"
               (-> jsonld (get "@graph") first (get "label"))))))))

(deftest load-test
  (testing "Load json-ld into database"
    (i/with-system [system ["drafter-client.edn" "cogs-staging.edn" "elasticsearch-test.edn"]]
      (let [datasets (example-datasets system)
            frame (slurp (io/resource "etl/dataset-frame.json"))
            jsonld (sut/transform frame datasets)
            indicies (idx/create-indicies system)
            result (sut/load-documents system "dataset" jsonld)]
        (is (= false (:errors result)))
        (is (= true (get-in (idx/delete-indicies system) [:dataset :acknowledged])))))))
