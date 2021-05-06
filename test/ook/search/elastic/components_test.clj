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

    (let [opts {:elastic/endpoint (:ook.concerns.elastic/endpoint system)}
          codelist-uris ["def/trade/concept-scheme/alcohol-type"
                         "def/trade/concept-scheme/bulletin-type"]]

      (testing "codelist-to-dimensions-lookup"
        (let [lookup (sut/codelist-to-dimensions-lookup codelist-uris opts)]
          (is (= {"def/trade/concept-scheme/alcohol-type"
                  ["def/trade/property/dimension/alcohol-type"]
                  "def/trade/concept-scheme/bulletin-type"
                  ["def/trade/property/dimension/bulletin-type"]}
                 lookup))))

      (testing "get-codelists"
        (let [codelists (sut/get-codelists codelist-uris opts)]
          (testing "provides uris"
            (is (= codelist-uris
                   (map :ook/uri codelists))))
          (testing "provides labels"
            (is (= '("Alcohol Type" "Bulletin Type")
                   (map :label codelists))))))

      (testing "components->codelists"
        (let [component-uris ["def/trade/property/dimension/alcohol-type"
                              "def/trade/property/dimension/bulletin-type"]]
          (is (=  [{:ook/uri "def/trade/concept-scheme/alcohol-type" :label "Alcohol Type"}
                   {:ook/uri "def/trade/concept-scheme/bulletin-type" :label "Bulletin Type"}]
                  (sut/components->codelists component-uris opts))))))))
