(ns ook.search.elastic.datasets-test
  (:require [ook.search.elastic.datasets :as sut]
            [clojure.test :refer [deftest testing is are]]))

(deftest observation-hits-test
  (let [facets {:name "Date"
                :dimensions ["data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dimension/period"
                             "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-production#dimension/period"]}
        results {:hits
                 {:hits
                  [{:_id "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts/year/2019/total-alcohol-duty-receipts/all"
                    :fields {(keyword "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dimension/period.@id")
                             ["http://reference.data.gov.uk/id/year/2019"],
                             (keyword "qb:dataSet.@id")
                             ["data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dataset"]}}
                   {:_id "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-production/government-year/1999-2000/uk-beer-production-alcohol/beer-and-cider"
                    :fields {(keyword "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-production#dimension/period.@id")
                             ["http://reference.data.gov.uk/id/government-year/1999-2000"]
                             (keyword "qb:dataSet.@id")
                             ["data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-production#dataset"]}}]}
                 :aggregations
                 {:datasets
                  {:buckets
                   [{:key "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-production#dataset",
                     :doc_count 735},
                    {:key "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dataset",
                     :doc_count 5}]}}}]

    (testing "datasets-from-observation-hits"
      (let [datasets (sut/datasets-from-observation-hits results)]

        (testing "Has a result for each dataset"
          (is (= ["data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-production#dataset"
                  "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dataset"]
                 (map :ook/uri datasets))))

        (testing "Provides an observation count for each dataset"
          (is (= [735
                  5]
                 (map :matching-observation-count datasets))))

        (testing "Provides example of matching value for each dimension"
          (is (= [{(keyword "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-production#dimension/period.@id")
                   "http://reference.data.gov.uk/id/government-year/1999-2000"}
                  {(keyword "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dimension/period.@id")
                   "http://reference.data.gov.uk/id/year/2019"}]
                 (map :matching-observation-example datasets))))))

    (testing "code-uris-from-observation-hits"
      (let [code-uris (sut/code-uris-from-observation-hits results)]

        (testing "Has a result for each code"
          (is (= ["http://reference.data.gov.uk/id/year/2019"
                  "http://reference.data.gov.uk/id/government-year/1999-2000"]
                 code-uris)))))))

(deftest explain-match-test
  (let [dataset-hits [{:ook/uri "cube1"
                       :matching-observation-example
                       {(keyword "date.@id") "2019"
                        (keyword "area.@id") "germany"}}
                      {:ook/uri "cube2"
                       :matching-observation-example
                       {(keyword "date.@id") "2020"
                        (keyword "area.@id") "canada"}}]
        facets [{:name "time" :dimensions ["date"]}
                {:name "place" :dimensions ["area"]}]
        codelists [{:ook/uri "years" :label "Years"}
                   {:ook/uri "countries" :label "Countries"}]
        codes [{:ook/uri "2019" :label "2019" :scheme "years"}
               {:ook/uri "2020" :label "2020" :scheme "years"}
               {:ook/uri "germany" :label "Germany" :scheme "countries"}
               {:ook/uri "canada" :label "Canada" :scheme "countries"}]
        datasets (sut/explain-match dataset-hits facets codelists codes)]

    (testing "removes :matching-observation-example"
      (is (= [nil nil]
             (map :matching-observation-example datasets))))

    (testing "adds :facets vector with one explanation per facet"
      (is (= [[{:name "time"
                :dimensions
                [{:ook/uri "date"
                  :codelists
                  [{:ook/uri "years"
                    :label "Years"
                    :examples
                    [{:ook/uri "2019"
                      :label "2019"}]}]}]}
               {:name "place"
                :dimensions
                [{:ook/uri "area"
                  :codelists
                  [{:ook/uri "countries"
                    :label "Countries"
                    :examples
                    [{:ook/uri "germany"
                      :label "Germany"}]}]}]}]

              [{:name "time"
                :dimensions
                [{:ook/uri "date"
                  :codelists
                  [{:ook/uri "years"
                    :label "Years"
                    :examples
                    [{:ook/uri "2020"
                      :label "2020"}]}]}]}
               {:name "place"
                :dimensions
                [{:ook/uri "area"
                  :codelists
                  [{:ook/uri "countries"
                    :label "Countries"
                    :examples
                    [{:ook/uri "canada"
                      :label "Canada"}]}]}]}]]
             (map :facets datasets)))))

  (testing "excludes codelist examples for dimensions that have none"
    (let [dataset-hits [{:ook/uri "cube1" :matching-observation-example {(keyword "date.@id") "2019"}}]
          facets [{:name "time" :dimensions ["date" "another-dimension"]}]
          codelists [{:ook/uri "years" :label "Years"}]
          codes [{:ook/uri "2019" :label "2019" :scheme "years"}
                 {:ook/uri "2020" :label "2020" :scheme "years"}]
          datasets (sut/explain-match dataset-hits facets codelists codes)]
      (is (= [[{:name "time"
                :dimensions
                [{:ook/uri "date"
                  :codelists [{:ook/uri "years"
                               :label "Years"
                               :examples
                               [{:ook/uri "2019" :label "2019"}]}]}
                 {:ook/uri "another-dimension"}]}]]
             (map :facets datasets))))))
