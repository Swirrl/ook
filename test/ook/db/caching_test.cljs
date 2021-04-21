(ns ook.db.caching-test
  (:require [cljs.test :refer [deftest is testing]]
            [ook.reframe.db.caching :as sut]))

(def initial-db
  {:facets/config {"facet 1" {:name "facet 1"}}})

(def codelist-1 {:ook/uri "cl1" :label "a codelist"})

(deftest cache-codelists-test
  (testing "it updates the right facet with a map of codelists indexed by uri"
    (is (= {"facet 1" {:name "facet 1"
                       :codelists {"cl1" codelist-1}}}
           (:facets/config
            (sut/cache-codelist initial-db "facet 1" [codelist-1]))))))

(deftest cache-code-tree-test
  (is (= {"facet 1" {:name "facet 1"
                     :codelists {"cl1" {:ook/uri "cl1"
                                        :label "a codelist"
                                        :children ["some children unchanged..."]}}}}

         (:facets/config
          (sut/cache-code-tree {:facets/config {"facet 1" {:name "facet 1" :codelists {"cl1" codelist-1}}}}
                               "facet 1"
                               "cl1"
                               ["some children unchanged..."])))))
