(ns ook.filters.selection-test
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
            "Facet 4" {:name "Facet 4" :sort-priority 4 :dimensions ["dim5"]}
            "Facet 5" {:name "Facet 5" :sort-priority 5 :dimensions ["dim 6"]}}
   :dataset-count 20})

(def codelists
  {"Facet 1" [{:ook/uri "cl1" :label "Codelist 1 Label"}
              {:ook/uri "another-codelist" :label "Another codelist"}]
   "Facet 2" [{:ook/uri "cl2" :label "Codelist 2 Label"}
              {:ook/uri "cl3" :label "Codelist 3 Label"}]
   "Facet 3" [{:ook/uri "no-codes" :label "Codelist no codes"}]
   "Facet 4" [{:ook/uri "deep-nested" :label "with nested codes"}]
   "Facet 5" [{:ook/uri "unused-branch" :label "with unused branch"}]})

(def concept-trees
  {"cl3" [{:scheme "cl3" :ook/uri "cl3-code1" :label "3-1 child 1" :used true}]
   "cl2" [{:scheme "cl2" :ook/uri "cl2-code1" :label "2-1 child 1" :used true :children nil}
          {:scheme "cl2" :ook/uri "cl2-code2" :label "2-1 child 2" :used true
           :children [{:scheme "cl2" :ook/uri "cl2-code3" :label "2-2 child 1" :used true}
                      {:scheme "cl2" :ook/uri "cl2-code4" :label "2-2 child 2" :used false}]}]
   "no-codes" []
   "deep-nested" [{:scheme "deep-nested" :ook/uri "cl4-code1" :label "4-1 child 1" :used true}
                  {:scheme "deep-nested" :ook/uri "cl5-code1" :label "5-1 child 1" :used true
                   :children [{:scheme "deep-nested" :ook/uri "cl5-code3" :label "5-2 child 1" :used true}
                              {:scheme "deep-nested" :ook/uri "cl5-code4" :label "5-2 child 2" :used true
                               :children [{:scheme "deep-nested" :ook/uri "cl5-code5" :label "5-3 child 1" :used true}]}]}]
   "unused-branch" [{:scheme "unused-branch" :ook/uri "cl6-code1" :label "unused branch" :used true
                     :children [{:scheme "unused-branch" :ook/uri "cl6-code2" :label "unused 1" :used false}
                                {:scheme "unused-branch" :ook/uri "cl6-code3" :label "unused 2" :used false}]}]})

(deftest selecting-codes
  (rft/run-test-sync
   (setup/stub-side-effects {:codelists codelists :concept-trees concept-trees})
   (setup/init! facets/configured-facets initial-state)
   (eh/click-text "Facet 2")

   (testing "fetching codes does not change selection"
     (eh/click-text "Codelist 2 Label")
     (is (= 1 (count (qh/all-selected-labels))))

     (eh/click (qh/find-expansion-toggle "Codelist 2 Label"))
     (is (= 1 (count (qh/all-selected-labels)))))

   (testing "fetching codes does not change disclosure"
     (testing "when a code in another codelist is already selected"
       (eh/click-text "2-1 child 1")
       (eh/click (qh/find-expansion-toggle "Codelist 2 Label"))
       (is (= ["Codelist 2 Label" "Codelist 3 Label"] (qh/all-checkbox-labels)))

       (eh/click (qh/find-expansion-toggle "Codelist 3 Label"))
       (is (= ["Codelist 2 Label" "Codelist 3 Label" "3-1 child 1"] (qh/all-checkbox-labels)))))

   (testing "toggling selection"
     (testing "works for individual codes"
       (eh/click (qh/find-expansion-toggle "Codelist 3 Label"))
       (eh/click (qh/find-expansion-toggle "Codelist 2 Label"))
       (is (= ["2-1 child 1"] (qh/all-selected-labels)))

       (eh/click (qh/find-expansion-toggle "2-1 child 2"))

       (eh/click-text "2-2 child 1")
       (is (= ["2-1 child 1" "2-2 child 1"] (qh/all-selected-labels)))

       (eh/click-text "2-1 child 1")
       (is (= ["2-2 child 1"] (qh/all-selected-labels))))

     (testing "un-selecting a code does not affect other codelists"
       (eh/click-text "Codelist 3 Label")
       (eh/click-text "2-2 child 1")
       (is (= ["Codelist 3 Label"] (qh/all-selected-labels)))
       (eh/click-text "Codelist 3 Label")))

   (testing "unused codes are not selectable"
     (is (= [] (qh/all-selected-labels)))
     (eh/click-text "2-2 child 2")
     (is (= [] (qh/all-selected-labels)))))

  (setup/cleanup!))

(deftest multiple-selection
  (rft/run-test-sync
   (setup/stub-side-effects {:codelists codelists :concept-trees concept-trees})

   (setup/init! facets/configured-facets initial-state)

   (eh/click-text "Facet 2")

   (testing "all codelists have an 'any' button"
     (is (not (nil? (qh/select-any-button "Codelist 2 Label"))))
     (is (not (nil? (qh/select-any-button "Codelist 3 Label")))))

   (testing "selecting any"
     (eh/click (qh/find-expansion-toggle "Codelist 2 Label"))
     (eh/click (qh/find-expansion-toggle "2-1 child 2"))

     (testing "collapses children"
       (is (= [] (qh/all-selected-labels)))
       (is (= 5 (count (qh/expanded-labels-under-label "Codelist 2 Label"))))

       (eh/click (qh/select-any-button "Codelist 2 Label"))
       (is (= 1 (count (qh/expanded-labels-under-label "Codelist 2 Label")))))

     (testing "selects parent and un-selects all children"
       (eh/click (qh/find-expansion-toggle "Codelist 2 Label"))
       (is (= 1 (count (qh/all-selected-labels))))

       (eh/click-text "2-1 child 1")
       (eh/click-text "2-2 child 1")
       (is (= 2 (count (qh/all-selected-labels))))

       (eh/click (qh/select-any-button "Codelist 2 Label"))
       (eh/click (qh/find-expansion-toggle "Codelist 2 Label"))
       (is (= 1 (count (qh/all-selected-labels))))))

   (testing "checking any child unselects the parent codelist"
     (eh/click-text "2-1 child 2")
     (is (= ["2-1 child 2"] (qh/all-selected-labels)))

     (eh/click-text "2-1 child 2"))

   (testing "selecting 'all children' or 'none'"
     (testing "does not show all children select button if no children are selectable"
       (eh/click-text "Facet 5")
       (eh/click (qh/find-expansion-toggle "with unused branch"))
       (eh/click (qh/find-expansion-toggle "unused branch"))
       (is (nil? (qh/multi-select-button "unused branch"))))

     (testing "selects all the children that are used"
       (eh/click-text "Facet 2")
       (eh/click (qh/find-expansion-toggle "Codelist 2 Label"))
       (eh/click (qh/find-expansion-toggle "2-1 child 2"))

       (eh/click (qh/multi-select-button "2-1 child 2"))
       (is (= ["2-2 child 1"] (qh/all-selected-labels))))

     (testing "unselects parent codelist if it was selected"
       (eh/click-text "Codelist 2 Label")

       (is (= ["Codelist 2 Label"] (qh/all-selected-labels)))
       (eh/click (qh/multi-select-button "2-1 child 2"))
       (is (= ["2-2 child 1"] (qh/all-selected-labels))))

     (testing "selects next-level children only for deeply nested code trees"
       (eh/click-text "2-2 child 1")
       (eh/click-text "Facet 4")
       (eh/click (qh/find-expansion-toggle "with nested codes"))
       (eh/click (qh/find-expansion-toggle "5-1 child 1"))
       (eh/click (qh/find-expansion-toggle "5-2 child 2"))

       (eh/click (qh/multi-select-button "5-1 child 1"))
       (is (= ["5-2 child 1" "5-2 child 2"] (qh/all-selected-labels))))

     (testing "shows option to select no children only if all children are selected"
       (is (= "none" (qh/text-content (qh/multi-select-button "5-1 child 1"))))
       (is (= "all children" (qh/text-content (qh/multi-select-button "5-2 child 2")))))

     (testing "expands to show children that were just selected"
       (eh/click (qh/find-expansion-toggle "5-2 child 2"))
       (is (qh/closed? (qh/find-expansion-toggle "5-2 child 2")))
       (is (= ["5-2 child 1" "5-2 child 2"] (qh/all-selected-labels)))

       (eh/click (qh/multi-select-button "5-2 child 2"))
       (is (= ["5-2 child 1" "5-2 child 2" "5-3 child 1"] (qh/all-selected-labels))))

     (testing "clears only children for that level and changes button label back"
       (is (= "none" (qh/text-content (qh/multi-select-button "5-2 child 2"))))

       (eh/click (qh/multi-select-button "5-2 child 2"))
       (is (= ["5-2 child 1" "5-2 child 2"] (qh/all-selected-labels)))
       (is (= "all children" (qh/text-content (qh/multi-select-button "5-2 child 2"))))

       (eh/click (qh/multi-select-button "5-1 child 1"))
       (is (= [] (qh/all-selected-labels))))))

  (setup/cleanup!))
