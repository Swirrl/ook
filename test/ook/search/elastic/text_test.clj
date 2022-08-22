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

    (let [opts {:elastic/conn (:ook.concerns.elastic/conn system)}]

      (testing "codes-to-selection returns dimension-value criteria"
        (let [codes (sut/codes "1994" opts)
              selection (sut/codes-to-selection codes opts)]
          (is (= {"data/gss_data/climate-change/met-office-annual-mean-rainfall-with-trends-actual#dimension/year"
                  ["http://reference.data.gov.uk/id/year/1994"]
                  "data/gss_data/climate-change/met-office-annual-mean-temp-with-trends-actual#dimension/year"
                  ["http://reference.data.gov.uk/id/year/1994"]
                  "data/gss_data/climate-change/met-office-annual-mean-temp-with-trends-anomaly#dimension/year"
                  ["http://reference.data.gov.uk/id/year/1994"]}
                 selection))))

      (testing "observation-hits returns results"
        (let [criteria {"data/gss_data/climate-change/met-office-annual-mean-rainfall-with-trends-actual#dimension/year"
                        ["http://reference.data.gov.uk/id/year/1994"
                         "http://reference.data.gov.uk/id/year/1995"
                         "http://reference.data.gov.uk/id/year/1996"]}
              observations (sut/observation-hits criteria opts)]
          (is (= 30
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
          (is (= 3 (count (sut/dataset-search "wales" opts))))
          (is (= "data/gss_data/climate-change/met-office-annual-mean-rainfall-with-trends-actual-catalog-entry"
                 (-> (sut/dataset-search "wales" opts) first :ook/uri))))

        (let [result (first (sut/dataset-search "wales" opts))]
          (testing "returns dataset metadata"
            (are [field value] (= value (get result field))
              :cube "data/gss_data/climate-change/met-office-annual-mean-rainfall-with-trends-actual#dataset"
              :label "Annual mean rainfall with trends actual"))

          (testing "adds observation counts"
            (is (= 318
                   (result :matching-observation-count))))

          (testing "adds matches to components"
            (let [components (:component result)]
              (testing "including all dimensions"
                (is (= ["Geography" "Measure type" "Year"]
                       (sort (map :label components)))))
              (testing "including codelists"
                (is (= "Geography"
                       (->>
                        components
                        (filter #(= "data/gss_data/climate-change/met-office-annual-mean-rainfall-with-trends-actual#dimension/geography"
                                    (:ook/uri %)))
                        first
                        :codelist
                        :label))))
              (testing "including matching codes"
                (let [matches (->>
                               components
                               (filter #(= "data/gss_data/climate-change/met-office-annual-mean-rainfall-with-trends-actual#dimension/geography"
                                           (:ook/uri %)))
                               first
                               :matches)
                      match (first matches)]
                  (are [field value] (= (get match field)
                                        value)
                    :ook/uri "data/gss_data/climate-change/met-office-annual-mean-rainfall-with-trends-actual#concept/geography/wales"
                    :label "Wales"))))))))))
