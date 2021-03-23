(ns ook.search.elastic-test
  (:require [clojure.test :refer [deftest testing is]]
            [ook.test.util.setup :as setup :refer [with-system]]
            [ook.search.elastic :as es]
            [ook.search.db :as sut]))

(defn get-db [system]
  (es/->Elasticsearch
   {:elastic/endpoint (:ook.concerns.elastic/endpoint system)
    :ook/facets (:ook.search/facets system)}))

(deftest database-test
  (testing "Extracting resources from the index"
    (with-system [system ["drafter-client.edn"
                          "cogs-staging.edn"
                          "elasticsearch-test.edn"
                          "fixture-data.edn"
                          "fixture-facets.edn"]]
      (setup/load-fixtures! system)

      (let [db (get-db system)]

        (testing "all datasets"
          (let [response (sut/all-datasets db)]
            (is (= 2 (count response)))
            (is (every? true? (map #(every? % [:comment :label :id :cube]) response)))
            (is (= ["Alcohol Bulletin - Duty Receipts"
                    "Alcohol Bulletin - Production"]
                   (map :label response)))))

        (testing "get-facets"
          (let [facets (sut/get-facets db)]
            (testing "Resolves parent-dimension to its sub-properties"
              (let [date (first (filter #(= "Date" (:name %)) facets))]
                (is (= 2 (count (:dimensions date))))
                (is (not (contains? date :parent-dimension)))))))

        (testing "get-components"
          (let [components (sut/get-components db "def/trade/property/dimension/alcohol-type")]
            (testing "Looks up components by URI(s)"
              (is (= "Alcohol Type"
                     (:label (first components)))))))

        (testing "get-datasets-for-components"
          (let [components ["data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-production#dimension/period"]
                datasets (sut/get-datasets-for-components db components)]
            (is (= ["Alcohol Bulletin - Production"]
                   (map :label datasets)))))

        (testing "get-datasets-for-facets"
          (let [facets {"Alcohol Type" "def/trade/concept-scheme/alcohol-type"
                        "Bulletin Type" "def/trade/concept-scheme/bulletin-type"}
                response (sut/get-datasets-for-facets db facets)]
            (testing "returns only matching datasets"
               ;; this test is non-effective since the only 2 datasets both match
              (is (= 2 (count response))))

            (testing "describes which codelists match for each facet"
              (let [ds (first response)]
                (is (= '("def/trade/concept-scheme/alcohol-type")
                       (get-in ds [:facet "Alcohol Type"])))
                (is (= '("def/trade/concept-scheme/bulletin-type")
                       (get-in ds [:facet "Bulletin Type"])))))))))))
