(ns ook.db.disclosure-test
  (:require [cljs.test :refer [deftest is]]
            [ook.reframe.codes.db.disclosure :as sut]))

(deftest expand-all-selected-codes-test
  (is (= #{}
         (sut/expand-all-selected-codes #{}
                                         {:facets/config {"facet" {:codelists [{:ook/uri "codelist1"
                                                                                :children []}]}}}
                                         {"codelist1" nil}
                                         "facet")))

  (is (= #{"codelist2"}
         (sut/expand-all-selected-codes
          #{}
          {:facets/config {"facet"
                           {:codelists
                            {"codelist1" {:ook/uri "codelist1" :children []}
                             "codelist2" {:ook/uri "codelist2" :children [{:ook/uri "code1"}]}}}}}
          {"codelist1" nil
           "codelist2" #{"code1"}}
          "facet")))

  (is (= #{"codelist2" "code1"}
         (sut/expand-all-selected-codes
          #{}
          {:facets/config {"facet"
                           {:codelists
                            {"codelist1" {:ook/uri "codelist1" :children []}
                             "codelist2"
                             {:ook/uri "codelist2"
                              :children [{:ook/uri "code1" :children [{:ook/uri "code2"}]}]}}}}}
          {"codelist1" nil
           "codelist2" #{"code2"}}
          "facet")))

  (is (= #{"codelist2" "code1" "code3" "code4"}
         (sut/expand-all-selected-codes
          #{}
          {:facets/config {"facet"
                           {:codelists
                            {"codelist1" {:ook/uri "codelist1" :children []}
                             "codelist2"
                             {:ook/uri "codelist2"
                              :children [{:ook/uri "code1" :children [{:ook/uri "code2"}]}
                                         {:ook/uri "code3"
                                          :children [{:ook/uri "code4" :children [{:ook/uri "code5"}]}]}]}}}}}
          {"codelist1" nil
           "codelist2" #{"code2" "code5"}}
          "facet"))))
