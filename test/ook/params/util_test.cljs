(ns ook.params.util-test
  (:require [cljs.test :refer [deftest is]]
            [ook.params.util :as sut]))

(deftest link-to-pmd-dataset
  (is (= "https://staging.gss-data.org.uk/cube/explore?uri=http%3A%2F%2Fgss-data.org.uk%2Fdata%2Fgss_data%2Ftrade%2Fons-uk-trade-in-goods-cpa-08-catalog-entry&apply-filters=true&filter-facets=http%253A%252F%252Fgss-data.org.uk%252Fdef%252Ftrade%252Fproperty%252Fdimension%252Fproduct%2Chttp%253A%252F%252Fgss-data.org.uk%252Fdef%252Ftrade%252Fconcept%252Fproduct%252FB"
         (sut/link-to-pmd-dataset
          "data/gss_data/trade/ons-uk-trade-in-goods-cpa-08-catalog-entry"
          [{:name "Product"
            :dimensions [{:ook/uri "def/trade/property/dimension/product"
                          :codelists [{:examples [{:ook/uri "def/trade/concept/product/B"}]}]}]}]))))
