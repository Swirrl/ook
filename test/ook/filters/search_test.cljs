(ns ook.filters.search-test
  (:require
   [cljs.test :refer-macros [deftest testing is]]
   [day8.re-frame.test :as rft]
   [ook.test.util.setup :as setup]

   [ook.test.util.event-helpers :as eh]
   [ook.test.util.query-helpers :as qh]
   [ook.reframe.facets.view :as facets]))

(def initial-state
  {:facets {"Facet 1" {:name "Facet 1" :sort-priority 1 :dimensions ["dim1" "dim2"]}
            "Facet 2" {:name "Facet 2" :sort-priority 2 :dimensions ["dim3"]}
            "Facet 3" {:name "Facet 3" :sort-priority 3 :dimensions ["dim4"]}
            "Facet 4" {:name "Facet 4" :sort-priority 4 :dimensions ["dim5"]}}
   :dataset-count 20})

(def codelists
  {"Facet 1" [{:ook/uri "cl1" :label "Codelist 1 Label"}
              {:ook/uri "another-codelist" :label "Another codelist"}]
   "Facet 2" [{:ook/uri "cl2" :label "Codelist 2 Label"}
              {:ook/uri "cl3" :label "Codelist 3 Label"}]
   "Facet 3" [{:ook/uri "cl5" :label "with nested codes"}]
   "Facet 4" [{:ook/uri "cl6" :label "with shared codes 1"}
              {:ook/uri "cl7" :label "with shared codes 2"}]})

(def concept-trees
  {"cl3" [{:scheme "cl3" :ook/uri "cl3-code1" :label "3-1 child 1" :used true}]

   "cl2" [{:scheme "cl2" :ook/uri "cl2-code1" :label "2-1 child 1" :used true :children nil}
          {:scheme "cl2" :ook/uri "cl2-code2" :label "2-1 child 2" :used true
           :children [{:scheme "cl2" :ook/uri "cl2-code3" :label "2-2 child 1" :used true}
                      {:scheme "cl2" :ook/uri "cl2-code4" :label "2-2 child 2" :used false}]}]

   "cl5" [{:scheme "cl4" :ook/uri "cl4-code1" :label "4-1 child 1" :used true}
          {:scheme "cl5" :ook/uri "cl5-code1" :label "5-1 child 1" :used true
           :children [{:scheme "cl5" :ook/uri "cl5-code3" :label "5-2 child 1" :used true}
                      {:scheme "cl5" :ook/uri "cl5-code4" :label "5-2 child 2" :used true
                       :children [{:scheme "cl5" :ook/uri "cl5-code5" :label "5-3 child 1" :used true}]}]}]

   "cl6" [{:scheme "cl6" :ook/uri "reused-code" :label "this code is reused" :used true}]

   "cl7" [{:scheme "cl7" :ook/uri "reused-code" :label "this code is reused" :used true}]})

(def search-results
  {"2-2 child 1" [{:scheme "cl2" :ook/uri "cl2-code3" :used true}]

   "2-2 child" [{:scheme "cl2" :ook/uri "cl2-code3"  :used true}
                {:scheme "cl2" :ook/uri "cl2-code4" :used false}]

   "5-3 child 1" [{:scheme "cl5" :ook/uri "cl5-code5" :used true}]

   "child 1" [{:scheme "cl5" :ook/uri "cl5-code1" :used true}
              {:scheme "cl5" :ook/uri "cl5-code3" :used true}
              {:scheme "cl5" :ook/uri "cl5-code5" :used true}]

   "this code is reused" [{:scheme "cl6" :ook/uri "reused-code" :used true}
                          {:scheme "cl7" :ook/uri "reused-code" :used true}]})

(defn- search-for [label]
  (eh/set-input-val (qh/code-search-input) label)
  (eh/click (qh/submit-search-button)))

(deftest searching-for-codes
  (rft/run-test-sync
   (setup/stub-side-effects {:codelists codelists
                             :concept-trees concept-trees
                             :search-results search-results})

   (setup/init! facets/configured-facets initial-state)

   (eh/click-text "Facet 2")

   (testing "search options are not visible if no search has been submitted"
     (is (nil? (qh/query-text "Select all matches")))
     (is (nil? (qh/query-text "Reset search"))))

   (testing "searching for a code with no matches"
     (is (= ["Codelist 2 Label" "Codelist 3 Label"] (qh/all-checkbox-labels)))
     (search-for "no matches!")

     (testing "shows a relevant message and reset button, no select all button"
       (is (nil? (qh/query-text "Select all matches")))
       (is (not (nil? (qh/query-text "Reset search"))))
       (is (not (nil? (qh/query-text "No codes match"))))
       (is (= [] (qh/all-checkbox-labels))))

     (testing "can be reset to show all the codelists again"
       (eh/click-text "Reset search")

       (is (nil? (qh/query-text "No codes match")))
       (is (= "" (qh/search-input-val)))
       (is (= ["Codelist 2 Label" "Codelist 3 Label"] (qh/all-checkbox-labels)))))

   (testing "searching for a code by label that matches"
     (is (nil? @setup/concept-tree-request))
     (search-for "2-2 child 1")

     (testing "shows select all and reset options"
       (is (not (nil? (qh/query-text "Select all matches"))))
       (is (not (nil? (qh/query-text "Reset search")))))

     (testing "fetches code trees that were not already cached"
       (is (= "cl2" @setup/concept-tree-request)))

     (testing "shows only matching codes as selectable but with all parents expanded"
       (is (= ["2-2 child 1"] (qh/all-checkbox-labels)))
       (is (= ["Codelist 2 Label" "2-1 child 2" "2-2 child 1"] (qh/all-labels)))

       (eh/click-text "Facet 3")
       (is (= ["with nested codes"] (qh/all-checkbox-labels)))
       (search-for "5-3 child 1")

       (is (= "cl5" @setup/concept-tree-request))
       (is (= [ "5-3 child 1"] (qh/all-checkbox-labels)))
       (is (= ["with nested codes" "5-1 child 1" "5-2 child 2" "5-3 child 1"] (qh/all-labels))))

     (testing "can be reset to show all the codelists again"
       (eh/click-text "Reset search")
       (is (= ["with nested codes"] (qh/all-checkbox-labels)))
       (is (qh/closed? (qh/find-expansion-toggle "with nested codes"))))

     (testing "it caches code trees"
       (eh/click-text "Facet 2")
       (eh/click (qh/find-expansion-toggle "Codelist 2 Label"))
       (is (= "cl5" @setup/concept-tree-request))))

   (testing "searching for a code that is in multiple codelists"
     (eh/click-text "Facet 4")
     (search-for "this code is reused")

     (testing "shows the result under both expanded codelists"
       (is (= ["with shared codes 1" "this code is reused" "with shared codes 2" "this code is reused"]
              (qh/all-labels)))))

   (testing "searching when the only thing that matches is a codelist itself"
     (search-for "with shared codes 1")

     (testing "does not include the codelist in search results"
       (is (= [] (qh/all-checkbox-labels))))))

  (setup/cleanup!))

(deftest code-search-results-selection-and-disclosure
  (rft/run-test-sync
   (setup/stub-side-effects {:codelists codelists
                             :concept-trees concept-trees
                             :search-results search-results})

   (setup/init! facets/configured-facets initial-state)

   (eh/click-text "Facet 2")

   (testing "Select all matches"
     (testing "selects all codes"
       (search-for "2-2 child 1")
       (is (= [] (qh/all-selected-labels)))

       (eh/click-text "Select all matches")
       (is (= ["2-2 child 1"] (qh/all-selected-labels)))

       (eh/click-text "2-2 child 1"))

     (testing "does not select unused codes"
       (search-for "2-2 child")
       (is (= [] (qh/all-selected-labels)))
       (is (= ["2-2 child 1" "2-2 child 2"] (qh/all-checkbox-labels)))

       (eh/click-text "Select all matches")
       (is (= ["2-2 child 1"] (qh/all-selected-labels))))

     (testing "is only enabled if some codes are selectable"
       (is (qh/disabled? (qh/query-text "Select all matches")))
       (eh/click-text "2-2 child 1")
       (is (not (qh/disabled? (qh/query-text "Select all matches"))))))

   (testing "Un-select all matches"
     (eh/click-text "Facet 3")
     (search-for "child 1")

     (testing "is disabled if nothing is selected"
       (is (= [] (qh/all-selected-labels)))
       (is (qh/disabled? (qh/query-text "Un-select all matches"))))

     (testing "is enabled if any (or all) codes are selected"
       (eh/click-text "5-1 child 1")
       (is (not (qh/disabled? (qh/query-text "Un-select all matches"))))

       (eh/click-text "Select all matches")
       (is (not (qh/disabled? (qh/query-text "Un-select all matches")))))

     (testing "un-selects all codes"
       (is (= ["5-1 child 1" "5-2 child 1" "5-3 child 1"] (qh/all-selected-labels)))
       (eh/click-text "Un-select all matches")
       (is (= [] (qh/all-selected-labels)))

       (eh/click-text "5-2 child 1")
       (is (= [ "5-2 child 1"] (qh/all-selected-labels)))

       (eh/click-text "Un-select all matches")
       (is (= [] (qh/all-selected-labels)))))

   (testing "interactions with previous selection and disclosure"
     (eh/click-text "Facet 2")

     (testing "clearing the search closes everything if no codes were selected"
       (is (= ["Codelist 2 Label" "Codelist 3 Label"] (qh/all-labels)))

       (search-for "2-2 child 1")
       (is (= ["Codelist 2 Label" "2-1 child 2" "2-2 child 1"] (qh/all-labels)))

       (eh/click-text "Reset search")
       (is (= ["Codelist 2 Label" "Codelist 3 Label"] (qh/all-labels))))

     (testing "clearing the search resets ui to expose all selected codes"
       (is (= ["Codelist 2 Label" "Codelist 3 Label"] (qh/all-labels)))
       (search-for "2-2 child 1")

       (eh/click-text "2-2 child 1")
       (eh/click-text "Reset search")
       (is (= ["Codelist 2 Label" "2-1 child 1" "2-1 child 2" "2-2 child 1" "2-2 child 2" "Codelist 3 Label"]
              (qh/all-labels)))

       (eh/click-text "2-2 child 1"))

     (testing "does not unselect any previously selected options"
       (eh/click (qh/find-expansion-toggle "Codelist 3 Label"))
       (eh/click-text "3-1 child 1")

       (is (= ["3-1 child 1"] (qh/all-selected-labels)))

       (search-for "2-2 child 1")
       (eh/click-text "Select all matches")
       (eh/click-text "Reset search")

       (is (= ["2-2 child 1" "3-1 child 1"] (qh/all-selected-labels)))
       (is (= ["Codelist 2 Label" "2-1 child 1" "2-1 child 2" "2-2 child 1" "2-2 child 2"
               "Codelist 3 Label" "3-1 child 1"]
              (qh/all-labels)))))

   (setup/cleanup!)))
