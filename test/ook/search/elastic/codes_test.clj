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

(deftest build-code-for-each-scheme-test
  (testing "it spreads a single code result into one for each scheme it has,
            excluding schemes that are not in the dimension"
    (is (= #{{:scheme "scheme-1"
              :ook/uri "code1" :label "label 1" :used true :children nil}
             {:scheme "scheme-2"
              :ook/uri "code1" :label "label 1" :used true :children nil}}
           (set
             (sut/build-code-for-each-scheme
                 {:_id "code1"
                  :_source {:label "label 1"
                            :used "true"
                            :scheme ["scheme-1" "scheme-2" "scheme-3"]}}
                 #{"scheme-1" "scheme-2"})))))

  (testing "it works with a single scheme"
    (is (= [{:ook/uri "code1" :label "label 1" :scheme "scheme-1" :used true :children nil}]
           (sut/build-code-for-each-scheme
            {:_id "code1"
             :_source {:label "label 1"
                       :used "true"
                       :scheme "scheme-1"}}
            #{"scheme-1"})))))
