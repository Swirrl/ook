(ns ook.search.elastic.text-test
  (:require [ook.search.elastic.text :as sut]
            [ook.test.util.setup :as setup :refer [with-system]]
            [clojure.test :refer [deftest testing is are]]))

(deftest dataset-search-test
  (with-system [system ["drafter-client.edn"
                        "idp-beta.edn"
                        "elasticsearch-test.edn"
                        "project/fixture/data.edn"
                        "project/fixture/facets.edn"]]

    ;;(setup/load-fixtures! system)

    (let [opts {:elastic/endpoint (:ook.concerns.elastic/endpoint system)}]

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

        (testing "returns rich snippet"
          (let [snippet (:snippet result)
                dimensions (:dimensions snippet)]
            (testing "including all dimensions"
              (is (= (sort ["Alcohol Type" "Alcohol Bulletin Type" "Measure type" "Period"])
                     (sort (map :label dimensions)))))
            (testing "including codelists"
              (is (= "Period"
                     (->>
                      dimensions
                      (filter #(= "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dimension/period"
                                  (:ook/uri %)))
                      first
                      :codelist
                      :label))))
            (testing "including matching codes"
              (let [matches (->>
                             dimensions
                             (filter #(= "def/trade/property/dimension/alcohol-type"
                                         (:ook/uri %)))
                             first
                             :codelist
                             :matches)]
                (is (= matches
                       [{:ook/uri "def/trade/concept/alcohol-type/wine"
                         :label "Wine"}
                        {:ook/uri "def/trade/concept/alcohol-type/made-wine"
                         :label "Made-Wine"}]))))))))))
