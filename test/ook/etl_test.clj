(ns ook.etl-test
  (:require [clojure.test :refer :all]
            [integrant.core :as ig]
            [ook.concerns.integrant :as i]
            [clj-http.client :as client]
            [vcr-clj.clj-http :refer [with-cassette]]
            [ook.etl :as sut]))

(deftest extract-test
  (testing "Extract triples from a drafter endpoint"
    (with-cassette :extract-datasets
      (let [system (i/exec-config {:profiles ["drafter-client.edn", "cogs-staging.edn"]})
            graphs ["http://gss-data.org.uk/graph/gss_data/trade/ons-exports-of-services-by-country-by-modes-of-supply-metadata"
                    "http://gss-data.org.uk/graph/gss_data/trade/ons-exports-of-services-by-country-by-modes-of-supply"]
            datasets (sut/extract-datasets system graphs)]
        (is (= 13 (count datasets)))
        (ig/halt! system)))))

(deftest transform-test
  (testing "Transform triples into json-ld"
    (with-cassette :extract-datasets
      (let [system (i/exec-config {:profiles ["drafter-client.edn", "cogs-staging.edn"]})
            graphs ["http://gss-data.org.uk/graph/gss_data/trade/ons-exports-of-services-by-country-by-modes-of-supply-metadata"
                    "http://gss-data.org.uk/graph/gss_data/trade/ons-exports-of-services-by-country-by-modes-of-supply"]
            datasets (sut/extract-datasets system graphs)
            jsonld (sut/transform-datasets datasets)]
        (is (= "Imports and Exports of services by country, by modes of supply"
               (-> jsonld (get "@graph") first (get "label"))
))
        (ig/halt! system)))))

(deftest load-test
  (testing "Load json-ld into database"
    (let [system (i/exec-config {:profiles ["drafter-client.edn"
                                            "cogs-staging.edn"
                                            "elasticsearch-local.edn"]})
          graphs ["http://gss-data.org.uk/graph/gss_data/trade/ons-exports-of-services-by-country-by-modes-of-supply-metadata"
                  "http://gss-data.org.uk/graph/gss_data/trade/ons-exports-of-services-by-country-by-modes-of-supply"]
          datasets (with-cassette :extract-datasets (sut/extract-datasets system graphs))
          jsonld (sut/transform-datasets datasets)
          result (sut/load-datasets system jsonld)]
      (is (= false (:errors result)))
      (ig/halt! system))))
