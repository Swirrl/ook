(ns ook.search.elastic-test
  (:require [clojure.test :refer [deftest testing is are]]
            [ook.test.util.setup :as setup :refer [with-system get-db]]
            [ook.search.db :as sut]))

(deftest database-test
  (testing "Extracting resources from the index"
    (with-system [system ["drafter-client.edn"
                          "cogs-staging.edn"
                          "elasticsearch-test.edn"
                          "project/fixture/data.edn"
                          "project/fixture/facets.edn"]]
      (setup/load-fixtures! system)

      (let [db (get-db system)]

        (testing "all datasets"
          (let [response (sut/all-datasets db)]
            (is (= 2 (count response)))
            (is (every? true? (map #(every? % [:comment :label :ook/uri :cube]) response)))
            (is (= ["Alcohol Bulletin - Duty Receipts"
                    "Alcohol Bulletin - Production"]
                   (map :label response)))))

        (testing "get-facets"
          (let [facets (sut/get-facets db)]
            (testing "Resolves parent-dimension to include itself and sub-properties"
              (let [date (first (filter #(= "Date" (:name %)) facets))]
                (is (= 3 (count (:dimensions date))))
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
          (testing "returns only matching datasets"
            (are [n codes] (= n (count (sut/get-datasets-for-facets db {"Alcohol Type"
                                                                        {"def/trade/concept-scheme/alcohol-type" codes}})))
              2 ["def/trade/concept/alcohol-type/beer-and-cider"]
              1 ["def/trade/concept/alcohol-type/wine"]
              2 ["def/trade/concept/alcohol-type/spirits"]))

          (testing "describes which codes match for each facet"
            (let [selections {"Alcohol Type" {"def/trade/concept-scheme/alcohol-type" ["def/trade/concept/alcohol-type/wine"]}
                              "Bulletin Type" {"def/trade/concept-scheme/bulletin-type" []}
                              }
                  response (sut/get-datasets-for-facets db selections)
                  dataset (first response)
                  description-for-facet (fn [name] (->> (:facets dataset)
                                                        (filter #(= (:name %) name))
                                                        first))] ;; switch to use map from facet name to details?
              (is (= {:name "Alcohol Type"
                      :dimensions
                      [{:ook/uri "def/trade/property/dimension/alcohol-type"
                        :codelists
                        [{:ook/uri "def/trade/concept-scheme/alcohol-type"
                          :label "Alcohol Type"
                          :examples
                          [{:ook/uri "def/trade/concept/alcohol-type/wine"
                            :ook/type "skos:Concept"
                            :label "Wine"
                            :broader nil
                            :narrower nil
                            :used "true"
                            :notation "wine"
                            :priority "1"}]}]}]}
                     (description-for-facet "Alcohol Type")))
              (is (= {:name "Bulletin Type"
                      :dimensions
                      [{:ook/uri "def/trade/property/dimension/bulletin-type"
                        :codelists
                        [{:ook/uri "def/trade/concept-scheme/bulletin-type"
                          :label "Bulletin Type"
                          :examples
                          [{:ook/uri
                            "def/trade/concept/bulletin-type/total-wine-duty-receipts"
                            :ook/type "skos:Concept"
                            :label "Total Wine Duty receipts"
                            :broader nil
                            :narrower nil
                            :used "true"
                            :notation "total-wine-duty-receipts"
                            :priority "8"}]}]}]}
                     (description-for-facet "Bulletin Type")))
              (is (= nil
                     (description-for-facet "Date"))))))))))
