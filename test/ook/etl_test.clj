(ns ook.etl-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clj-http.core]
            [vcr-clj.clj-http :as vcr]
            [ook.test.util.setup :as setup :refer [with-system]]
            [ook.index :as idx]
            [ook.etl :as sut]
            [ook.etl :as etl]
            [ook.search.db :as db]
            [ook.util :as util]))

(deftest extract-test
  (testing "Extracting a page of RDF from a drafter endpoint"
    (with-system [system ["drafter-client.edn" "idp-beta.edn"]]
      (is (= 36 (count (setup/example-datasets system)))))))

(deftest transform-test
  (testing "Transform triples into json-ld"
    (with-system [system ["drafter-client.edn", "idp-beta.edn"]]
      (let [datasets (setup/example-datasets system)
            frame (slurp (io/resource "etl/dataset-frame.json"))
            jsonld (sut/transform frame datasets)]
        (is (= "Alcohol Bulletin - Clearances"
               (-> jsonld (get "@graph") first (get "label"))))))))

(deftest load-test
  (testing "Load json-ld into database"
    (with-system [system ["drafter-client.edn" "idp-beta.edn" "elasticsearch-test.edn"]]
      (let [datasets (setup/example-datasets system)
            frame (slurp (io/resource "etl/dataset-frame.json"))
            jsonld (sut/transform frame datasets)
            result (sut/load-documents system "dataset" jsonld)]
        (is (= false (:errors (first result))))
        (is (= true (get-in (idx/delete-indicies system) [:dataset :acknowledged])))))))

(defn found?
  "Checks to see if the doc has the value for the key.

  You can specify nil values to check for the existence of a
  property in the index mapping as otherwise :missing-from-mapping
  would be returned"
  [doc key value]
  (= value
     (get doc key :missing-from-mapping)))

(deftest dataset-pipeline-test
  (testing "Dataset pipeline schema"
    (with-system [system ["drafter-client.edn"
                          "idp-beta.edn"
                          "elasticsearch-test.edn"
                          "project/fixture/data.edn"]]
      (setup/reset-indicies! system)
      (vcr/with-cassette {:name :dataset-pipeline :recordable? setup/not-localhost?}
        (etl/dataset-pipeline system))

      (let [db (setup/get-db system)
            doc (first (db/all-datasets db))]
        (are [key value] (found? doc key value)
          :ook/uri "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts-catalog-entry"
          :label "Alcohol Bulletin - Duty Receipts"
          :publisher {:altlabel "HMRC"})))))

(deftest component-pipeline-test
  (testing "Component pipeline schema"
    (with-system [system ["drafter-client.edn"
                          "idp-beta.edn"
                          "elasticsearch-test.edn"]]
      (setup/reset-indicies! system)
      (vcr/with-cassette {:name :component-pipeline :recordable? setup/not-localhost?}
        (etl/component-pipeline system))

      (let [db (setup/get-db system)
            doc (first (db/get-components db ["def/trade/property/dimension/alcohol-type"]))]
        (are [key value] (found? doc key value)
          :ook/uri "def/trade/property/dimension/alcohol-type"
          :label "Alcohol Type"
          :codelist {:ook/uri "def/trade/concept-scheme/alcohol-type"
                     :label "Alcohol Type"})))))

(deftest code-pipeline-test
  (testing "Code pipeline schema"
    (with-system [system ["drafter-client.edn"
                          "idp-beta.edn"
                          "elasticsearch-test.edn"
                          "project/fixture/data.edn"]]
      (setup/reset-indicies! system)
      (vcr/with-cassette {:name :code-pipeline :recordable? setup/not-localhost?}
        (etl/code-pipeline system)
        (etl/code-used-pipeline system))

      (let [db (setup/get-db system)
            doc (first (db/get-codes db ["def/trade/concept/alcohol-type/beer"]))]
        (are [key value] (found? doc key value)
          :ook/uri "def/trade/concept/alcohol-type/beer"
          :label "Beer"
          :notation "beer"
          :used "false"
          :narrower nil
          :broader nil
          :topConceptOf "def/trade/concept-scheme/alcohol-type")))))

(deftest observation-pipeline-test
  (testing "Observation pipeline schema"
    (with-system [system ["drafter-client.edn"
                          "local.edn"
                          "elasticsearch-test.edn"
                          "project/fixture/data.edn"]]
      (setup/reset-indicies! system)
      ;; TODO vcr
      (is (= 4206 (etl/pipeline system)))
      ;; TODO modify just one graph, run pipeline again, assert fewer changes
      )))
