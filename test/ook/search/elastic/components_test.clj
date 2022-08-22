(ns ook.search.elastic.components-test
  (:require [ook.search.elastic.components :as sut]
            [ook.test.util.setup :as setup :refer [with-system]]
            [clojure.test :refer [deftest testing is]]))

(deftest components-tests
  (with-system [system ["drafter-client.edn"
                        "idp-beta.edn"
                        "elasticsearch-test.edn"
                        "project/fixture/data.edn"
                        "project/fixture/facets.edn"]]

    (setup/load-fixtures! system)

    (let [opts {:elastic/conn (:ook.concerns.elastic/conn system)}
          codelist-uris ["data/gss_data/climate-change/met-office-annual-mean-temp-with-trends-actual#scheme/geography"
                         "data/gss_data/climate-change/met-office-annual-mean-temp-with-trends-actual#scheme/year"]]

      (testing "codelist-to-dimensions-lookup"
        (let [lookup (sut/codelist-to-dimensions-lookup codelist-uris opts)]
          (is (= {"data/gss_data/climate-change/met-office-annual-mean-temp-with-trends-actual#scheme/geography"
                  ["data/gss_data/climate-change/met-office-annual-mean-temp-with-trends-actual#dimension/geography"]
                  "data/gss_data/climate-change/met-office-annual-mean-temp-with-trends-actual#scheme/year"
                  ["data/gss_data/climate-change/met-office-annual-mean-temp-with-trends-actual#dimension/year"]}
                 lookup))))

      (testing "get-codelists"
        (let [codelists (sut/get-codelists codelist-uris opts)]
          (testing "provides uris"
            (is (= codelist-uris
                   (map :ook/uri codelists))))
          (testing "provides labels"
            (is (= ["Geography" "Year"]
                   (map :label codelists))))))

      (testing "components->codelists"
        (let [component-uris ["data/gss_data/climate-change/met-office-annual-mean-temp-with-trends-actual#dimension/geography"
                              "data/gss_data/climate-change/met-office-annual-mean-temp-with-trends-actual#dimension/year"]]
          (is (= [{:ook/uri "data/gss_data/climate-change/met-office-annual-mean-temp-with-trends-actual#scheme/geography" :label "Geography"}
                  {:ook/uri "data/gss_data/climate-change/met-office-annual-mean-temp-with-trends-actual#scheme/year" :label "Year"}]
                 (sut/components->codelists component-uris opts))))))))
