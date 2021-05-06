(ns ook.db.disclosure-test
  (:require [cljs.test :refer [deftest testing is]]
            [ook.reframe.codes.db.disclosure :as sut]))

(deftest expand-all-selected-codes-test
  (testing "works when selection is just a codelist"
    (is (= #{}
           (sut/expand-all-selected-codes #{}
                                          {:facets/config {"facet" {:codelists [{:ook/uri "codelist1"
                                                                                 :children :no-children}]}}}
                                          {"codelist1" nil}
                                          "facet"))))

  (testing "works for a codelist with a selection"
    (is (= #{"codelist2"}
           (sut/expand-all-selected-codes
            #{}
            {:facets/config {"facet"
                             {:codelists
                              {"codelist1" {:ook/uri "codelist1" :children :no-children}
                               "codelist2" {:ook/uri "codelist2" :children [{:ook/uri "code1"}]}}}}}
            {"codelist1" nil
             "codelist2" #{"code1"}}
            "facet"))))

  (testing "works with a codelist and codelist with code selection"
    (is (= #{"codelist2" "code1"}
           (sut/expand-all-selected-codes
            #{}
            {:facets/config {"facet"
                             {:codelists
                              {"codelist1" {:ook/uri "codelist1" :children :no-children}
                               "codelist2"
                               {:ook/uri "codelist2"
                                :children [{:ook/uri "code1" :children [{:ook/uri "code2"}]}]}}}}}
            {"codelist1" nil
             "codelist2" #{"code2"}}
            "facet"))))

  (testing "works for multiple code selections"
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

  (testing "does not clobber any existing disclosure"
    (is (= #{"already-expanded" "codelist2"}
           (sut/expand-all-selected-codes
            #{"already-expanded"}
            {:facets/config {"facet"
                             {:codelists
                              {"codelist1" {:ook/uri "codelist1" :children :no-children}
                               "codelist2" {:ook/uri "codelist2" :children [{:ook/uri "code1"}]}}}}}
            {"codelist1" nil
             "codelist2" #{"code1"}}
            "facet"))))

  (testing "works when a codelist has no children"
    (is (= #{"codelist2"}
           (sut/expand-all-selected-codes
            #{}
            {:facets/config {"facet"
                             {:codelists
                              {"codelist1" {:ook/uri "codelist1" :children :no-children}
                               "codelist2"
                               {:ook/uri "codelist2"
                                :children [{:ook/uri "code1" :children [{:ook/uri "code2"}]}
                                           {:ook/uri "code3"
                                            :children [{:ook/uri "code4" :children [{:ook/uri "code5"}]}]}]}}}}}
            {"codelist1" nil
             "codelist2" #{"code1"}}
            "facet")))))
