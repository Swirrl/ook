(ns ook.search.elastic-test
  (:require [clojure.test :refer [deftest testing is are]]
            [ook.test.util.setup :as setup :refer [with-system get-db]]
            [ook.search.db :as sut]))

(deftest database-test
  (testing "Extracting resources from the index"
    (with-system [system ["drafter-client.edn"
                          "idp-beta.edn"
                          "elasticsearch-test.edn"
                          "project/fixture/data.edn"
                          "project/fixture/facets.edn"]]
      (setup/load-fixtures! system)

      (let [db (get-db system)]

        (testing "all datasets"
          (let [response (sut/all-datasets db)]
            (is (= 2 (count response)))
            (is (every? true? (map #(every? % [:comment :label :ook/uri]) response)))
            (is (= ["Alcohol Bulletin - Duty Receipts"
                    "Alcohol Bulletin - Production"]
                   (map :label response)))))

        (testing "get-facets"
          (testing "Resolves parent-dimension to add itself and sub-properties to dimensions"
            (let [facets (sut/get-facets db)
                  date (first (filter #(= "Date" (:name %)) facets))]
              (is (= 3 (count (:dimensions date))))
              (is (not (contains? date :parent-dimension)))))
          (testing "Child dimensions don't replace any specific dimensions"
            (let [system (assoc system :ook.search/facets [{:name "Date"
                                                            :sort-priority 4
                                                            :parent-dimension "sdmxd:refPeriod"
                                                            :dimensions ["specific-time-dimension"]}])
                  db (get-db system)
                  facets (sut/get-facets db)
                  date (first (filter #(= "Date" (:name %)) facets))]
              (is (contains? (set (:dimensions date)) "specific-time-dimension")))))

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
          (testing "returns only those which include the codes"
            (are [n codes] (= n (count (sut/get-datasets-for-facets db {"Alcohol Type"
                                                                        {"def/trade/concept-scheme/alcohol-type" codes}})))
              2 ["def/trade/concept/alcohol-type/beer-and-cider"]
              1 ["def/trade/concept/alcohol-type/wine"]
              2 ["def/trade/concept/alcohol-type/spirits"]))

          (testing "codes may appear in any dimension/ codelist within the same facet (combined with OR/ should)"
            (let [selections {"Date" ;; both fixture datasets use the same period scheme (just with different dimensions)
                              {"data/gss_data/trade/hmrc-alcohol-bulletin#scheme/period"
                               ["http://reference.data.gov.uk/id/year/2020"]}}]
              (is (= 2 (count (sut/get-datasets-for-facets db selections))))))
          (testing "codes must appear for each and every facet (combined with AND/ must)"
            (let [selections {"Date"
                              {"data/gss_data/trade/hmrc-alcohol-bulletin#scheme/period"
                               ["http://reference.data.gov.uk/id/year/2020"]}
                              "Alcohol Type" ;; only one dataset has wine and thus conforms to both facets
                              {"def/trade/concept-scheme/alcohol-type"
                               ["def/trade/concept/alcohol-type/wine"]}}]
              (is (= 1 (count (sut/get-datasets-for-facets db selections))))))

          (testing "includes dataset metadata"
            (let [selections {"Alcohol Type"
                              {"def/trade/concept-scheme/alcohol-type"
                               ["def/trade/concept/alcohol-type/wine"]}}
                  dataset (first (sut/get-datasets-for-facets db selections))]
              (are [field value] (= value (field dataset))
                :comment "Monthly Duty Receipts statistics from the 4 different alcohol duty regimes administered by HM Revenue and Customs"
                :publisher {:altlabel "HMRC"})))

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
                        :label "Alcohol Type"
                        :codes
                        [{:ook/uri "def/trade/concept/alcohol-type/wine"
                          :ook/type "skos:Concept"
                          :narrower nil
                          :broader nil
                          :topConceptOf "def/trade/concept-scheme/alcohol-type"
                          :scheme
                          [{:ook/uri "def/trade/concept-scheme/alcohol-type"
                            :label "Alcohol Type"}]
                          :used "true"
                          :notation "wine"
                          :label "Wine"
                          :priority "1"}]}]}
                     (description-for-facet "Alcohol Type")))
              (is (= {:name "Bulletin Type"
                      :dimensions
                      [{:ook/uri "def/trade/property/dimension/bulletin-type"
                        :label "Alcohol Bulletin Type"
                        :codes
                        [{:ook/uri "def/trade/concept/bulletin-type/total-alcohol-duty-receipts"
                          :ook/type "skos:Concept"
                          :priority "9"
                          :label "Total alcohol duty receipts"
                          :narrower nil
                          :broader nil
                          :topConceptOf "def/trade/concept-scheme/bulletin-type"
                          :notation "total-alcohol-duty-receipts"
                          :scheme
                          [{:ook/uri "def/trade/concept-scheme/bulletin-type"
                            :label "Bulletin Type"}]
                          :used "true"}
                         {:ook/uri "def/trade/concept/bulletin-type/total-wine-duty-receipts"
                          :ook/type "skos:Concept"
                          :priority "8"
                          :label "Total Wine Duty receipts"
                          :narrower nil
                          :broader nil
                          :topConceptOf "def/trade/concept-scheme/bulletin-type"
                          :notation "total-wine-duty-receipts"
                          :scheme
                          [{:ook/uri "def/trade/concept-scheme/bulletin-type"
                            :label "Bulletin Type"}]
                          :used "true"}]}]}
                     (description-for-facet "Bulletin Type")))
              (is (= nil
                     (description-for-facet "Date"))))))))))
