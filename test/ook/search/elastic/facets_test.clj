(ns ook.search.elastic.facets-test
  (:require [ook.search.elastic.facets :as sut]
            [clojure.test :refer [deftest testing is are]]))

(deftest apply-selections-test
  (testing "Replaces dimension URIs with their docs"
    (let [components [{:ook/uri "fruit" :codelist {:ook/uri "fruits"}}]
          facets [{:name "food" :dimensions ["fruit"]}]
          selections {"food" ["fruits"]}
          selected-facets (sut/apply-selections facets components selections)
          selected-dimension-ids (->> selected-facets first :dimensions (map :ook/uri))]
      (is (= '("fruit")
             selected-dimension-ids))))
  (testing "Filters dimensions based upon selections"
    (let [components [{:ook/uri "fruit" :codelist {:ook/uri "fruits"}}
                      {:ook/uri "veg" :codelist {:ook/uri "vegetables"}}]
          facets [{:name "food" :dimensions ["fruit" "veg"]}]
          selections {"food" ["vegetables"]}
          selected-facets (sut/apply-selections facets components selections)
          selected-dimension-ids (->> selected-facets first :dimensions (map :ook/uri))]
      (is (= '("veg")
             selected-dimension-ids)))))

(deftest apply-facets-test
  (let [components [{:ook/uri "year" :codelist {:ook/uri "years" }}
                    {:ook/uri "quarter" :codelist {:ook/uri "quarters"}}
                    {:ook/uri "area" :codelist {:ook/uri "areas"}}]
        datasets [{:ook/uri "data-by-area" :component ["area"]}
                  {:ook/uri "data-by-year" :component ["year"]}
                  {:ook/uri "data-by-year-and-quarter" :component ["year" "quarter"]}]
        facets [{:name "date" :dimensions ["year" "quarter"]}
                {:name "location" :dimensions ["area"]}]
        search (partial sut/apply-facets datasets components facets)]
    (testing "Find datasets that have the dimensions"
      (let [names (fn [results] (->> results (sort-by :ook/uri) (map :ook/uri)))]
        (is (= '("data-by-year" "data-by-year-and-quarter")
               (names (search {"date" ["years"]}))))
        (is (= '("data-by-year-and-quarter")
               (names (search {"date" ["quarters"]}))))))
    (testing "Add facet attribute for comparing datasets"
      (testing "Merges facet into dataset result"
        (is (= [{:ook/uri "data-by-area"
                 :component ["area"]
                 :facets [{:name "location"
                           :dimensions [{:ook/uri "area" :codelist {:ook/uri "areas"}}]}]}]
               (search {"location" ["areas"]}))))
      (testing "Filters facet for selections"
        (is (= [{:ook/uri "data-by-year-and-quarter"
                 :component ["year" "quarter"]
                 :facets [{:name "date"
                           :dimensions [{:ook/uri "quarter" :codelist {:ook/uri "quarters"}}]}]}]
               (search {"date" ["quarters"]})))))))
