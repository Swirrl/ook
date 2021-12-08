(ns ook.ui.layout-test
  (:require [clojure.test :refer [deftest testing is]]
            [ook.ui.layout :as sut]
            [clojure.string :refer [includes?]]))

(defn the-page [data]
  (sut/->html (sut/search {} data)))

(deftest search-results-test
  (testing "empty state"
    (testing "before as query is submitted"
      (is (not (includes? (the-page {}) "Found"))))
    (testing "when no results come back"
      (is (includes? (the-page {:query "unobtainium"
                                :datasets []})
                     "Found 0 results")))))
