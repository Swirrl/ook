(ns ook.etl-test
  (:require [clojure.test :refer :all]
            [integrant.core :as ig]
            [ook.concerns.integrant :as i]
            [ook.index :as idx]
            [clj-http.client :as client]
            [vcr-clj.clj-http :refer [with-cassette]]
            [ook.etl :as sut]))

(def test-graphs
  ["http://gss-data.org.uk/graph/gss_data/trade/ons-exports-of-services-by-country-by-modes-of-supply-metadata"
   "http://gss-data.org.uk/graph/gss_data/trade/ons-exports-of-services-by-country-by-modes-of-supply"])

(deftest extract-test
  (testing "Extract triples from a drafter endpoint"
    (i/with-system [system ["drafter-client.edn", "cogs-staging.edn"]]
      (let [datasets (with-cassette :extract-datasets (sut/extract-datasets system test-graphs))]
        (is (= 13 (count datasets)))))))

(deftest transform-test
  (testing "Transform triples into json-ld"
    (i/with-system [system ["drafter-client.edn", "cogs-staging.edn"]]
      (let [datasets (with-cassette :extract-datasets (sut/extract-datasets system test-graphs))
            jsonld (sut/transform-datasets datasets)]
        (is (= "Imports and Exports of services by country, by modes of supply"
               (-> jsonld (get "@graph") first (get "label"))))))))

(deftest load-test
  (testing "Load json-ld into database"
    (i/with-system [system ["drafter-client.edn" "cogs-staging.edn" "elasticsearch-local.edn"]]
      (let [datasets (with-cassette :extract-datasets (sut/extract-datasets system test-graphs))
            jsonld (sut/transform-datasets datasets)
            indicies (idx/create-indicies system)
            result (sut/load-datasets system jsonld)]
        (is (= false (:errors result)))
        (is (= true (get-in (idx/delete-indicies system) [:dataset :acknowledged])))))))
