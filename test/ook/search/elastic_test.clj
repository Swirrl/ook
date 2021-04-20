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
          (testing "Resolves parent-dimension to add itself and sub-properties to dimensions"
            (let [facets (sut/get-facets db)
                  date (first (filter #(= "Date" (:name %)) facets))]
              (is (= 3 (count (:dimensions date))))
              (is (not (contains? date :parent-dimension)))))
          (testing "Child dimensions don't replace any specific dimensions"
            (let [system (assoc system :ook.search/facets [{:name "Date"
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
          (testing "returns only matching datasets"
            (are [n codes] (= n (count (sut/get-datasets-for-facets db {"Alcohol Type"
                                                                        {"def/trade/concept-scheme/alcohol-type" codes}})))
              2 ["def/trade/concept/alcohol-type/beer-and-cider"]
              1 ["def/trade/concept/alcohol-type/wine"]
              2 ["def/trade/concept/alcohol-type/spirits"]))

          (testing "includes dataset metadata"
            (let [selections {"Alcohol Type"
                              {"def/trade/concept-scheme/alcohol-type"
                               ["def/trade/concept/alcohol-type/wine"]}}
                  dataset (first (sut/get-datasets-for-facets db selections))]
              (are [field value] (= value (field dataset))
                :description "Monthly Duty Receipts statistics from the 4 different alcohol duty regimes administered by HM Revenue and Customs
 Table of historic wine, made wine, spirits, beer and cider Duty Receipts"
                :publisher {:ook/uri "https://www.gov.uk/government/organisations/hm-revenue-customs"
                            :label "HM Revenue & Customs"
                            :altlabel "HMRC"})))

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
                          :label "Wine"
                          :broader nil
                          :narrower nil
                          :topConceptOf "def/trade/concept-scheme/alcohol-type"
                          :used "true"
                          :notation "wine"
                          :priority "1"
                          :scheme
                          [{:ook/uri "def/trade/concept-scheme/alcohol-type"
                            :label "Alcohol Type"}]}]}]}
                     (description-for-facet "Alcohol Type")))
              (is (= {:name "Bulletin Type"
                      :dimensions
                      [{:ook/uri "def/trade/property/dimension/bulletin-type"
                        :label "Alcohol Bulletin Type"
                        :codes
                        [{:ook/uri
                          "def/trade/concept/bulletin-type/total-wine-duty-receipts"
                          :ook/type "skos:Concept"
                          :label "Total Wine Duty receipts"
                          :broader nil
                          :narrower nil
                          :topConceptOf "def/trade/concept-scheme/bulletin-type"
                          :used "true"
                          :notation "total-wine-duty-receipts"
                          :priority "8"
                          :scheme
                          [{:ook/uri "def/trade/concept-scheme/bulletin-type"
                            :label "Bulletin Type"}]}]}]}
                     (description-for-facet "Bulletin Type")))
              (is (= nil
                     (description-for-facet "Date"))))))))))
