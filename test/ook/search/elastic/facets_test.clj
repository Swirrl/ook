(ns ook.search.elastic.facets-test
  (:require [ook.search.elastic.facets :as sut]
            [ook.util :refer [id]]
            [clojure.test :refer [deftest testing is are]]))

(deftest apply-selections-test
  (testing "Replaces dimension URIs with their docs"
    (let [components [{id "fruit" :codelist "fruits"}]
          facets [{:name "food" :dimensions ["fruit"]}]
          selections {"food" ["fruits"]}
          selected-facets (sut/apply-selections facets components selections)
          selected-dimension-ids (->> selected-facets first :dimensions (map id))]
      (is (= '("fruit")
             selected-dimension-ids))))
  (testing "Filters dimensions based upon selections"
    (let [components [{id "fruit" :codelist "fruits"}
                      {id "veg" :codelist "vegetables"}]
          facets [{:name "food" :dimensions ["fruit" "veg"]}]
          selections {"food" ["vegetables"]}
          selected-facets (sut/apply-selections facets components selections)
          selected-dimension-ids (->> selected-facets first :dimensions (map id))]
      (is (= '("veg")
             selected-dimension-ids)))))

(deftest apply-facets-test
  (let [components [{id "year" :codelist "years"}
                    {id "quarter" :codelist "quarters"}
                    {id "area" :codelist "areas"}]
        datasets [{id "data-by-area" :component ["area"]}
                  {id "data-by-year" :component ["year"]}
                  {id "data-by-year-and-quarter" :component ["year" "quarter"]}]
        facets [{:name "date" :dimensions ["year" "quarter"]}
                {:name "location" :dimensions ["area"]}]
        search (partial sut/apply-facets datasets components facets)]
    (testing "Find datasets that have the dimensions"
      (let [names (fn [results] (->> results (sort-by id) (map id)))]
        (is (= '("data-by-year" "data-by-year-and-quarter")
               (names (search {"date" ["years"]}))))
        (is (= '("data-by-year-and-quarter")
               (names (search {"date" ["quarters"]}))))))
    (testing "Add facet attribute for comparing datasets"
      (testing "Merges facet into dataset result"
        (is (= [{id "data-by-area"
                 :component ["area"]
                 :facets [{:name "location"
                           :dimensions [{id "area" :codelist "areas"}]}]}]
               (search {"location" ["areas"]}))))
      (testing "Filters facet for selections"
        (is (= [{id "data-by-year-and-quarter"
                 :component ["year" "quarter"]
                 :facets [{:name "date"
                           :dimensions [{id "quarter" :codelist "quarters"}]}]}]
               (search {"date" ["quarters"]})))))))


