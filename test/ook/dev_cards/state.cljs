(ns ook.dev-cards.state)

(def search-results
  {:initial {:result {:codes {:query "test"
                              :data [{:id "http://example.test"
                                      :label "Test code label"}]}}
             :ui {:codes {:query ""}}}

   "missing" {:result {:codes {:query "missing"
                               :data []}}
              :ui {:codes {:query "missing"}}}

   "england" {:ui {:codes {:query "england"}}
              :result {:codes {:query "england"
                               :data [{:id "def/concept/nuts-geography/nuts1/UKH",
                                       :label "East of England"}
                                      {:id "def/concept/service-origin-geography/nuts1/ukh",
                                       :label "East of England"}
                                      {:id "def/concept/service-origin-geography/west-of-england",
                                       :label "West of England"}]}}}

   "driver" {:ui {:codes {:query "driver"}}
             :result {:codes {:query  "driver"
                              :data [{:id "http://stamina-project.org/codes/cpav2008/subcategory/49.32.12",
                                      :label "Rental services of passenger cars with driver"}
                                     {:id "http://stamina-project.org/codes/cpav2008/subcategory/49.39.31",
                                      :label "Rental services of buses and coaches with driver"}
                                     {:id "http://stamina-project.org/codes/cpav2008/subcategory/77.12.11",
                                      :label
                                      "Rental and leasing services of goods transport vehicles without driver"}
                                     {:id "https://trade.ec.europa.eu/def/cn_2012#hs4_8702",
                                      :label "Motor vehicles for the transport of >= 10 persons, incl. driver"}
                                     {:id "http://stamina-project.org/codes/cpav2008/subcategory/77.12.19",
                                      :label
                                      "Rental and leasing services of other land transport equipment without driver"}
                                     {:id "def/concept/sitc-4/783.1",
                                      :label
                                      "Motor vehicles for the transport of ten or more persons, including the driver"}
                                     {:id "https://trade.ec.europa.eu/def/cn_2012#cn8_87029090",
                                      :label
                                      "Motor vehicles for the transport of >= 10 persons, incl. driver, not with internal combustion piston engine"}
                                     {:id "https://trade.ec.europa.eu/def/cn_2012#hs6_870210",
                                      :label
                                      "Motor vehicles for the transport of >= 10 persons, incl. driver, with compression-ignition internal combustion piston engine \"diesel or semi-diesel engine\""}
                                     {:id "https://trade.ec.europa.eu/def/cn_2012#cn8_87029011",
                                      :label
                                      "Motor vehicles for the transport of >= 10 persons, incl. driver, with spark-ignition internal combustion piston engine, of a cylinder capacity of > 2.800 cm³, new"}
                                     {:id "https://trade.ec.europa.eu/def/cn_2012#cn8_87029019",
                                      :label
                                      "Motor vehicles for the transport of >= 10 persons, incl. driver, with spark-ignition internal combustion piston engine, of a cylinder capacity of > 2.800 cm³, used"}
                                     {:id "https://trade.ec.europa.eu/def/cn_2012#cn8_87029031",
                                      :label
                                      "Motor vehicles for the transport of >= 10 persons, incl. driver, with spark-ignition internal combustion piston engine, of a cylinder capacity of <= 2.800 cm³, new"}
                                     {:id "https://trade.ec.europa.eu/def/cn_2012#cn8_87029039",
                                      :label
                                      "Motor vehicles for the transport of >= 10 persons, incl. driver, with spark-ignition internal combustion piston engine, of a cylinder capacity of <= 2.800 cm³, used"}]}}}})
