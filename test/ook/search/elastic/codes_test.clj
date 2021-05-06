(ns ook.search.elastic.codes-test
  (:require [ook.search.elastic.codes :as sut]
            [ook.test.util.setup :as setup :refer [with-system]]
            [clojure.test :refer [deftest testing is]]))

(deftest codes-tests
  (with-system [system ["drafter-client.edn"
                        "idp-beta.edn"
                        "elasticsearch-test.edn"
                        "project/fixture/data.edn"
                        "project/fixture/facets.edn"]]

    (setup/load-fixtures! system)

    (let [opts {:elastic/endpoint (:ook.concerns.elastic/endpoint system)}
          codelist-uri "def/trade/concept-scheme/bulletin-type"
          tree (sut/build-concept-tree codelist-uri opts)]

      (testing "build-concept-tree"
        (is (= 30 (count tree)))
        (is (= (-> tree first keys sort)
               [:children :label :scheme :used :ook/uri]))
        (is (= ["1.2% to 5.5% ABV clearances" "Cider clearances"]
               (->> tree (map :label) sort (take 2))))))))
