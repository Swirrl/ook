(ns ook.etl-test
  (:require [clojure.test :refer :all]
            [integrant.core :as ig]
            [ook.concerns.integrant :as i]
            [clj-http.client :as client]
            [vcr-clj.clj-http :refer [with-cassette]]
            [ook.etl :as sut]))

(deftest extract-test
  (testing "Extract data from a drafter endpoint"
    (with-cassette :extract-datasets
      (let [system (i/exec-config {:profiles ["drafter-client.edn", "cogs-staging.edn"]})
            graphs ["http://gss-data.org.uk/graph/gss_data/trade/ons-exports-of-services-by-country-by-modes-of-supply-metadata"
                    "http://gss-data.org.uk/graph/gss_data/trade/ons-exports-of-services-by-country-by-modes-of-supply"]
            datasets (sut/extract-datasets system graphs)]
        (is (= 13 (count datasets)))
        (ig/halt! system)))))
