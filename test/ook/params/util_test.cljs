(ns ook.params.util-test
  (:require [cljs.test :refer [deftest is]]
            [ook.params.util :as sut]))

(deftest link-to-pmd-dataset
  (is (= "https://staging.gss-data.org.uk/cube/explore?uri=http%3A%2F%2Fgss-data.org.uk%2Fdata%2Fgss_data%2Ftrade%2Fhmrc-alcohol-bulletin%2Falcohol-bulletin-clearances-catalog-entry&filters-drawer=open&filter-facets=http%253A%252F%252Fgss-data.org.uk%252Fdef%252Ftrade%252Fproperty%252Fdimension%252Falcohol-type%2Chttp%253A%252F%252Fgss-data.org.uk%252Fdef%252Ftrade%252Fconcept%252Falcohol-type%252Fmade-wine&filter-facets=http%253A%252F%252Fgss-data.org.uk%252Fdef%252Ftrade%252Fproperty%252Fdimension%252Fbulletin-type%2Chttp%253A%252F%252Fgss-data.org.uk%252Fdef%252Ftrade%252Fconcept%252Fbulletin-type%252Ftotal-made-wine-clearances"
         (sut/link-to-pmd-dataset
          "data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-clearances-catalog-entry"
          [{:value "def/trade/concept/alcohol-type/made-wine"
            :dimension "def/trade/property/dimension/alcohol-type"}
           {:value "def/trade/concept/bulletin-type/total-made-wine-clearances"
            :dimension "def/trade/property/dimension/bulletin-type"}]))))
