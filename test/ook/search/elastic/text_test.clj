(ns ook.search.elastic.text-test
  (:require [ook.search.elastic.text :as sut]
            [ook.test.util.setup :as setup :refer [with-system]]
            [clojure.test :refer [deftest testing is are]]))

(deftest text-test
  (with-system [system ["drafter-client.edn"
                        "idp-beta.edn"
                        "elasticsearch-test.edn"
                        "project/fixture/data.edn"
                        "project/fixture/facets.edn"]]

    (setup/load-fixtures! system)

    (let [opts {:elastic/endpoint (:ook.concerns.elastic/endpoint system)}]

      (testing "codes-to-selection returns dimension-value criteria"
        (let [codes (sut/codes "beer" opts)
              selection (sut/codes-to-selection codes opts)]
          (is (= {"def/trade/property/dimension/alcohol-type"
                  ["def/trade/concept/alcohol-type/beer-and-cider"]
                  "def/trade/property/dimension/bulletin-type"
	          ["def/trade/concept/bulletin-type/total-beer-clearances"
	           "def/trade/concept/bulletin-type/uk-beer-production"
	           "def/trade/concept/bulletin-type/total-beer-clearances-alcohol"
	           "def/trade/concept/bulletin-type/total-beer-duty-receipts"
	           "def/trade/concept/bulletin-type/uk-beer-production-alcohol"]}
                 selection))))

      (testing "observation-hits returns results"
        (let [criteria {"data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-production#dimension/period"
                        ["http://reference.data.gov.uk/id/government-year/1999-2000"
                         "http://reference.data.gov.uk/id/government-year/2000-2001"
                         "http://reference.data.gov.uk/id/government-year/2001-2002"]
                        "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dimension/period"
                        ["http://reference.data.gov.uk/id/government-year/1999-2000"
                         "http://reference.data.gov.uk/id/government-year/2000-2001"
                         "http://reference.data.gov.uk/id/government-year/2001-2002"]}
              observations (sut/observation-hits criteria opts)]
          (is (= 24
                 (get-in observations [:hits :total :value])))))

      (testing "ordered datasets"
        (let [datasets [{:ook/uri "match-only-one-dimension"
                         :component [{:ook/uri "space"}
                                     {:ook/uri "time" :matches [{:ook/uri "today"}]}]}
                        {:ook/uri "match-both-dimensions"
                         :component [{:ook/uri "space" :matches [{:ook/uri "earth"}]}
                                     {:ook/uri "time" :matches [{:ook/uri "today"}]}]}]]
          (is (= ["match-both-dimensions"
                  "match-only-one-dimension"]
                 (map :ook/uri (sut/ordered datasets))))))

      (testing "dataset-search"

        (testing "matches datasets"
          (is (= 2 (count (sut/dataset-search "beer" opts))))
          (is (= "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts-catalog-entry"
                 (-> (sut/dataset-search "wine" opts) first :ook/uri))))

        (let [result (first (sut/dataset-search "wine" opts))]
          (testing "returns dataset metadata"
            (are [field value] (= value (get result field))
              :cube "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dataset"
              :label "Alcohol Bulletin - Duty Receipts"
              :comment "Monthly Duty Receipts statistics from the 4 different alcohol duty regimes administered by HM Revenue and Customs"))

          (testing "adds observation counts"
            (is (= 304
                   (result :matching-observation-count))))

          (testing "adds matches to components"
            (let [components (:component result)]
              (testing "including all dimensions"
                (is (= (sort ["Alcohol Type" "Alcohol Bulletin Type" "Measure type" "Period"])
                       (sort (map :label components)))))
              (testing "including codelists"
                (is (= "Period"
                       (->>
                        components
                        (filter #(= "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dimension/period"
                                    (:ook/uri %)))
                        first
                        :codelist
                        :label))))
              (testing "including matching codes"
                (let [matches (->>
                               components
                               (filter #(= "def/trade/property/dimension/alcohol-type"
                                           (:ook/uri %)))
                               first
                               :matches)
                      match (first matches)]
                  (are [field value] (= (get match field)
                                        value)
                    :ook/uri "def/trade/concept/alcohol-type/wine"
                    :label "Wine"))))))))))
