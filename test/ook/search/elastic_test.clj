(ns ook.search.elastic-test
  (:require [clojure.test :refer [deftest testing is]]
            [ook.test.util.setup :as setup :refer [with-system]]
            [ook.search.elastic :as es]
            [ook.search.db :as sut]))

(defn get-db [es-endpoint]
  (es/->Elasticsearch {:elastic/endpoint es-endpoint}))

(deftest database-test
  (testing "Extracting resources from the index"
    (with-system [system ["drafter-client.edn"
                          "cogs-staging.edn"
                          "elasticsearch-test.edn"
                          "fixture-data.edn"]]
      (setup/load-fixtures! system)

      (testing "all datasets"
        (let [db (get-db (:ook.concerns.elastic/endpoint system))
              response (sut/all-datasets db)]
          (is (= 2 (count response)))
          (is (every? true? (map #(every? % [:comment :label :id :cube]) response)))
          (is (= ["Alcohol Bulletin - Duty Receipts"
                  "Alcohol Bulletin - Production"]
                 (map :label response))))))))
