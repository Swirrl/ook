(ns ook.params.parse-test
  (:require [clojure.test :refer [deftest testing is]]
            [ook.params.parse :as sut]))

(deftest parse-named-facets-test
  (testing "it converts query params to a selection properly formatted"
    (let [req {:query-params {"facet" ["facet name" "codelist1" "codelist2"]}}
          result (sut/parse-named-facets req)]
      (is (= result {"facet name" ["codelist1" "codelist2"]})))))
