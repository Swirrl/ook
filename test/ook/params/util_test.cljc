(ns ook.params.util-test
  (:require [clojure.test :refer [deftest is testing]]
            [ook.params.util :as sut]))

(deftest pmd-link-from-facets-test
  (testing "works for matches with all expected data"
    (is (= "https://beta.gss-data.org.uk/cube/explore?uri=http%3A%2F%2Fgss-data.org.uk%2Fdata%2Fgss_data%2Ftrade%2Fons-uk-trade-in-goods-cpa-08-catalog-entry&apply-filters=true&qb-filters=http%3A%2F%2Fgss-data.org.uk%2Fdef%2Ftrade%2Fproperty%2Fdimension%2Fproduct%7Cihttp%3A%2F%2Fgss-data.org.uk%2Fdef%2Ftrade%2Fconcept%2Fproduct%2FA%7Cihttp%3A%2F%2Fgss-data.org.uk%2Fdef%2Ftrade%2Fconcept%2Fproduct%2FB"
           (sut/pmd-link-from-facets
            "data/gss_data/trade/ons-uk-trade-in-goods-cpa-08-catalog-entry"
            [{:name "Product"
              :dimensions [{:ook/uri "def/trade/property/dimension/product"
                            :codes [{:ook/uri "def/trade/concept/product/A" :scheme [{:ook/uri "def/trade/concept-scheme/product"}]}
                                    {:ook/uri "def/trade/concept/product/B" :scheme [{:ook/uri "def/trade/concept-scheme/product"}]}]}]}]
            {"Product" {"def/trade/concept-scheme/product" ["def/trade/concept/product/A" "def/trade/concept/product/B"]}}))))

  (testing "does not include filter facet params for facets with no matching codes"
    (is (= "https://beta.gss-data.org.uk/cube/explore?uri=http%3A%2F%2Fgss-data.org.uk%2Fdata%2Fdataset-id"
           (sut/pmd-link-from-facets
            "data/dataset-id"
            [{:name "Facet"
              :dimensions [{:ook/uri "some/dim"
                             ;; no matching codes!
                            }]}]
            {"Facet" {"some/scheme" ["some/code"]}}))))

  (testing "does not include filter facet params for facets specified with codelists (despite finding matching codes)"
    (is (= "https://beta.gss-data.org.uk/cube/explore?uri=http%3A%2F%2Fgss-data.org.uk%2Fdata%2Fdataset-id"
           (sut/pmd-link-from-facets
            "data/dataset-id"
            [{:name "Facet"
              :dimensions [{:ook/uri "some/dim"
                            :codes [{:ook/uri "matching/code"  :scheme [{:ook/uri "some/scheme"}]}]}]}]
            {"Facet" {"some/scheme" []}})))))

(deftest pmd-link-from-dataset-test
  (testing "includes matching codes"
    (is (= "https://beta.gss-data.org.uk/cube/explore?uri=http%3A%2F%2Fgss-data.org.uk%2Fdata%2Fgss_data%2Ftrade%2Fons-uk-trade-in-goods-cpa-08-catalog-entry&apply-filters=true&qb-filters=http%3A%2F%2Fgss-data.org.uk%2Fdef%2Ftrade%2Fproperty%2Fdimension%2Fproduct%7Cihttp%3A%2F%2Fgss-data.org.uk%2Fdef%2Ftrade%2Fconcept%2Fproduct%2FA%7Cihttp%3A%2F%2Fgss-data.org.uk%2Fdef%2Ftrade%2Fconcept%2Fproduct%2FB"
           (sut/pmd-link-from-dataset
            {:ook/uri "data/gss_data/trade/ons-uk-trade-in-goods-cpa-08-catalog-entry"
             :component
             [{:ook/uri "def/trade/property/dimension/product"
               :matches
               [{:ook/uri "def/trade/concept/product/A"}
                {:ook/uri "def/trade/concept/product/B"}]}]}))))
  (testing "ignores dimensions without matching codes"
    (is (= "https://beta.gss-data.org.uk/cube/explore?uri=http%3A%2F%2Fgss-data.org.uk%2Fdata%2Fdataset-id"
           (sut/pmd-link-from-dataset
            {:ook/uri "data/dataset-id"
             :component
             [{:ook/uri "some/dim"}]})))))

(deftest absolute-uri-test
  (testing "reverses json-ld prefixing"
    (testing "adds base to gss-data URIs"
      (is (= "http://gss-data.org.uk/example"
             (sut/absolute-uri "example"))))
    (testing "doesn't add base to absolute URIs"
      (is (= "http://example.com"
             (sut/absolute-uri "http://example.com"))))))
