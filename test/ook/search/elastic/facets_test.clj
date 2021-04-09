(ns ook.search.elastic.facets-test
  (:require [ook.search.elastic.facets :as sut]
            [clojure.test :refer [deftest testing is are]]))

(deftest dimension-selections-test
  (let [codelist-selections {"food" {"fruits" ["apple"]
                                     "vegetables" []}
                             "drink" {"beers" ["ale" "kölsch"]
                                      "tea" ["assam"]}}
        dimensions-lookup {"fruits" ["ate"]
                           "vegetables" ["ate"]
                           "beers" ["drank"]
                           "tea" ["drank"]}
        dimension-selections (sut/dimension-selections codelist-selections dimensions-lookup)]
    (testing "codelists are replaced with dimensions"
      (is (= ["ate" "drank"]
             (keys dimension-selections))))
    (testing "dimension values from multiple codelists are combined"
      (is (= ["ale" "kölsch" "assam"]
             (dimension-selections "drank"))))
    (testing "wildcard selection (empty vector) overrides specific codes (apple)"
      (is (= (dimension-selections "ate")
             [])))))
