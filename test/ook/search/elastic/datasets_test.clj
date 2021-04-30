(ns ook.search.elastic.datasets-test
  (:require [ook.search.elastic.datasets :as sut]
            [clojure.test :refer [deftest testing is]]))

(deftest observation-hits-test
  ;; results returned by private method (here we're testing parsing not the query construction which is integration tested in elastic-test) 
  #_(sut/find-observations {"data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dimension/period" []
                          "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-production#dimension/period" []}
                         {:elastic/endpoint "http://localhost:9201"})
  (let [results {:aggregations
                 {:datasets
                  {:buckets ;; NB: each dataset only finds observations to aggregate with it's own dimensions
                   [{:key "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-production#dataset",
                     :doc_count 735
                     (keyword "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-production#dimension/period.@id")
                     {:buckets
                      [{:key
                        "http://reference.data.gov.uk/id/government-year/1999-2000",
                        :doc_count 3}
                       {:key
                        "http://reference.data.gov.uk/id/government-year/2000-2001",
                        :doc_count 3}
                       {:key
                        "http://reference.data.gov.uk/id/government-year/2001-2002",
                        :doc_count 3}]},
                     (keyword "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dimension/period.@id")
                     {:buckets []}},
                    {:key "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dataset",
                     :doc_count 1520
                     (keyword "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-production#dimension/period.@id")
                     {:buckets []},
                     (keyword "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dimension/period.@id")
                     {:buckets
                      [{:key
                        "http://reference.data.gov.uk/id/government-year/1999-2000",
                        :doc_count 5}
                       {:key
                        "http://reference.data.gov.uk/id/government-year/2000-2001",
                        :doc_count 5}
                       {:key
                        "http://reference.data.gov.uk/id/government-year/2001-2002",
                        :doc_count 5}]}}]}}}]

    (testing "datasets-from-observation-hits"
      (let [datasets (sut/datasets-from-observation-hits results)]

        (testing "Has a result for each dataset"
          (is (= ["data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-production#dataset"
                  "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dataset"]
                 (map :ook/uri datasets))))

        (testing "Provides an observation count for each dataset"
          (is (= [735
                  1520]
                 (map :matching-observation-count datasets))))

        (testing "Provides examples of matching values for each dimension"
          (is (= [{(keyword "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-production#dimension/period.@id")
                   ["http://reference.data.gov.uk/id/government-year/1999-2000"
                    "http://reference.data.gov.uk/id/government-year/2000-2001"
                    "http://reference.data.gov.uk/id/government-year/2001-2002"]}
                  {(keyword "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dimension/period.@id")
                   ["http://reference.data.gov.uk/id/government-year/1999-2000"
                    "http://reference.data.gov.uk/id/government-year/2000-2001"
                    "http://reference.data.gov.uk/id/government-year/2001-2002"]}]
                 (map :matching-dimension-values datasets))))

        (testing "code-uris-from-datasets"
          (let [code-uris (sut/code-uris-from-datasets datasets)]

            (testing "Has a result for each code"
              (is (= ["http://reference.data.gov.uk/id/government-year/1999-2000"
                      "http://reference.data.gov.uk/id/government-year/2000-2001"
                      "http://reference.data.gov.uk/id/government-year/2001-2002"]
                     code-uris)))))))))

(deftest explain-match-test
  (let [dataset-hits [{:ook/uri "cube1"
                       :matching-dimension-values
                       {(keyword "date.@id") ["2019"]
                        (keyword "area.@id") ["germany"]}}
                      {:ook/uri "cube2"
                       :matching-dimension-values
                       {(keyword "date.@id") ["2020"]
                        (keyword "area.@id") ["canada"]}}]
        facets [{:name "time" :dimensions ["date"]}
                {:name "place" :dimensions ["area"]}]
        dimensions [{:ook/uri "date" :label "Date"}
                    {:ook/uri "area" :label "Area"}]
        codelists [{:ook/uri "years" :label "Years"}
                   {:ook/uri "countries" :label "Countries"}]
        codes [{:ook/uri "2019" :label "2019" :scheme "years"}
               {:ook/uri "2020" :label "2020" :scheme "years"}
               {:ook/uri "germany" :label "Germany" :scheme "countries"}
               {:ook/uri "canada" :label "Canada" :scheme "countries"}]
        datasets (sut/explain-match dataset-hits facets dimensions codelists codes)]

    (testing "removes :matching-dimension-values"
      (is (= [nil nil]
             (map :matching-dimension-values datasets))))

    (testing "adds :facets vector with one explanation per facet"
      (is (= [[{:name "time"
                :dimensions
                [{:ook/uri "date"
                  :label "Date"
                  :codes
                  [{:ook/uri "2019"
                    :label "2019"
                    :scheme
                    [{:ook/uri "years"
                      :label "Years"}]}]}]}
               {:name "place"
                :dimensions
                [{:ook/uri "area"
                  :label "Area"
                  :codes
                  [{:ook/uri "germany"
                    :label "Germany"
                    :scheme
                    [{:ook/uri "countries"
                      :label "Countries"}]}]}]}]

              [{:name "time"
                :dimensions
                [{:ook/uri "date"
                  :label "Date"
                  :codes
                  [{:ook/uri "2020"
                    :label "2020"
                    :scheme
                    [{:ook/uri "years"
                      :label "Years"}]}]}]}
               {:name "place"
                :dimensions
                [{:ook/uri "area"
                  :label "Area"
                  :codes
                  [{:ook/uri "canada"
                    :label "Canada"
                    :scheme
                    [{:ook/uri "countries"
                      :label "Countries"}]}]}]}]]
             (map :facets datasets)))))

  (testing "excludes code examples for dimensions or whole facets that have none"
    (let [dataset-hits [{:ook/uri "cube1" :matching-dimension-values {(keyword "date.@id") ["2019"]}}]
          facets [{:name "time" :dimensions ["date" "another-dimension"]}
                  {:name "place" :dimensions ["area"]}]
          dimensions [{:ook/uri "date" :label "Date"}
                      {:ook/uri "area" :label "Area"}
                      {:ook/uri "another-dimension" :label "Another Dimension"}]
          codelists [{:ook/uri "years" :label "Years"}]
          codes [{:ook/uri "2019" :label "2019" :scheme "years"}
                 {:ook/uri "2020" :label "2020" :scheme "years"}]
          datasets (sut/explain-match dataset-hits facets dimensions codelists codes)]
      (is (= [[{:name "time"
                :dimensions
                [{:ook/uri "date"
                  :label "Date"
                  :codes
                  [{:ook/uri "2019"
                    :label "2019"
                    :scheme [{:ook/uri "years"
                              :label "Years"}]}]}]}]]
             (map :facets datasets)))))

  (testing "excludes codes from dimensions when there are no matches (no nested nils)"
    (let [dataset-hits [{:ook/uri "cube1" :matching-dimension-values {(keyword "date.@id") ["2019"]}}]
          facets [{:name "time" :dimensions ["date"]}]
          dimensions [{:ook/uri "date" :label "Date"}]
          codelists [{:ook/uri "years" :label "Years"}]
          codes []
          datasets (sut/explain-match dataset-hits facets dimensions codelists codes)]
      (is (= [{:name "time"
                :dimensions [{:ook/uri "date" :label "Date"}]}]
             (mapcat :facets datasets))))))
