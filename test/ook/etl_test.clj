(ns ook.etl-test
  (:require [clojure.test :refer :all]
            [integrant.core :as ig]
            [ook.concerns.integrant :as i]
            [ook.etl :as sut]))

(deftest extract-test
  (testing "Extract data from a drafter endpoint"
    (let [system (i/exec-config {:profiles ["drafter-client.edn", "cogs-staging.edn"]})
          datasets (sut/extract-datasets system)]
      (is (= 10 (count datasets)))
      (ig/halt! system))))
