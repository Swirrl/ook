(ns ook.search.elastic.facets-test
  (:require [ook.search.elastic.facets :as sut]
            [clojure.test :refer [deftest testing is are]]))

(deftest dimension-selections-test
  (let [codelist-selections {"food" {"fruits" ["apple"]
                                     "vegetables" []}
                             "drink" {"beers" ["ale" "kölsch"]
                                      "tea" ["assam"]}}
        dimensions-lookup {"fruits" ["ate"] ;; codelist to dimensions
                           "vegetables" ["ate"]
                           "beers" ["drank"]
                           "tea" ["drank"]}
        dimension-selections (sut/dimension-selections codelist-selections dimensions-lookup)]
    (testing "codelists are replaced with dimensions"
      (is (= ["ate"] (keys (get dimension-selections "food"))))
      (is (= ["drank"] (keys (get dimension-selections "drink")))))
    (testing "dimension values from multiple codelists are combined"
      (is (= ["ale" "kölsch" "assam"]
             (get-in dimension-selections ["drink" "drank"]))))
    (testing "wildcard selection (empty vector) overrides specific codes (apple)"
      (is (= (get-in dimension-selections ["food" "ate"])
             [])))))
