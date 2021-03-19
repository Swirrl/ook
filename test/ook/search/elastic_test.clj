(ns ook.search.elastic-test
  (:require [clojure.test :refer [deftest testing is]]
            [ook.test.util.setup :as setup :refer [with-system]]
            [ook.search.elastic :as es]
            [ook.search.db :as sut]))

(defn get-db [es-endpoint]
  (es/->Elasticsearch {:elastic/endpoint es-endpoint}))

(deftest database-test
  (testing "Extracting a page of RDF from a drafter endpoint"
    (with-system [system ["drafter-client.edn", "cogs-staging.edn" "elasticsearch-test.edn"]]
      (setup/load-datasets! system)

      (testing "all datasets"
        (let [db (get-db (:ook.concerns.elastic/endpoint system))
              response (sut/all-datasets db)]
          (is (= 3 (count response)))
          (is (every? true? (map #(every? % [:comment :label :id :cube]) response)))
          (is (= ["Alcohol Bulletin - Clearances"
                  "Alcohol Bulletin - Duty Receipts"
                  "Alcohol Bulletin - Production"]
                 (map :label response))))))))