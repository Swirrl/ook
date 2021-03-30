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
    (with-system [system ["drafter-client.edn" "cogs-staging.edn"]]
      (is (= 33 (count (setup/example-datasets system)))))))

(deftest transform-test
  (testing "Transform triples into json-ld"
    (with-system [system ["drafter-client.edn", "cogs-staging.edn"]]
      (let [datasets (setup/example-datasets system)
            frame (slurp (io/resource "etl/dataset-frame.json"))
            jsonld (sut/transform frame datasets)]
        (is (= "Alcohol Bulletin - Clearances"
               (-> jsonld (get "@graph") first (get "label"))))))))

(deftest load-test
  (testing "Load json-ld into database"
    (with-system [system ["drafter-client.edn" "cogs-staging.edn" "elasticsearch-test.edn"]]
      (let [datasets (setup/example-datasets system)
            frame (slurp (io/resource "etl/dataset-frame.json"))
            jsonld (sut/transform frame datasets)
            result (sut/load-documents system "dataset" jsonld)]
        (is (= false (:errors (first result))))
        (is (= true (get-in (idx/delete-indicies system) [:dataset :acknowledged])))))))

(deftest components-test
  (testing "Components pipeline schema"
    (with-system [system ["drafter-client.edn"
                          "cogs-staging.edn"
                          "elasticsearch-test.edn"]]
      (setup/reset-indicies! system)
      (vcr/with-cassette {:name :extract-components :recordable? setup/not-localhost?}
        (etl/component-pipeline system))

      (let [db (setup/get-db system)
            doc (first (db/get-components db ["def/trade/property/dimension/alcohol-type"]))]
        (are [key value] (= value (key doc))
          :ook/uri "def/trade/property/dimension/alcohol-type"
          :label "Alcohol Type"
          :codelist {:ook/uri "def/trade/concept-scheme/alcohol-type"
                     :label "Alcohol Type"})))))
