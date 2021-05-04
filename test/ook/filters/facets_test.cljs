(ns ook.filters.facets-test
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
            "Facet 5" {:name "Facet 5" :sort-priority 4 :dimensions ["dim5"]}}
   :dataset-count 20})

(def codelists
  {"Facet 1" [{:ook/uri "cl1" :label "Codelist 1 Label"}
              {:ook/uri "another-codelist" :label "Another codelist"}]
   "Facet 2" [{:ook/uri "cl2" :label "Codelist 2 Label"}
              {:ook/uri "cl3" :label "Codelist 3 Label"}]
   "Facet 3" [{:ook/uri "no-codes" :label "Codelist no codes"}]
   "Facet 4" [{:ook/uri "deep-nested" :label "with nested codes"}]
   "Facet 5" []})

(def concept-trees
  {"cl3" [{:scheme "cl3" :ook/uri "cl3-code1" :label "3-1 child 1" :used true}]
   "cl2" [{:scheme "cl2" :ook/uri "cl2-code1" :label "2-1 child 1" :used true :children nil}
          {:scheme "cl2" :ook/uri "cl2-code2" :label "2-1 child 2" :used true
           :children [{:scheme "cl2" :ook/uri "cl2-code3" :label "2-2 child 1" :used true}
                      {:scheme "cl2" :ook/uri "cl2-code4" :label "2-2 child 2" :used false}]}]
   "no-codes" []
   "deep-nested" [{:scheme "cl4" :ook/uri "cl4-code1" :label "4-1 child 1" :used true}
                  {:scheme "cl5" :ook/uri "cl5-code1" :label "5-1 child 1" :used true
                   :children [{:scheme "cl5" :ook/uri "cl5-code3" :label "5-2 child 1" :used true}
                              {:scheme "cl5" :ook/uri "cl5-code4" :label "5-2 child 2" :used true
                               :children [{:scheme "cl5" :ook/uri "cl5-code5" :label "5-3 child 1" :used true}]}]}]})

(deftest selecting-facets
  (rft/run-test-sync

   (setup/stub-side-effects {:codelists codelists :concept-trees concept-trees})

   (setup/init! facets/configured-facets initial-state)

   (testing "clear facet selection button only shows when a facet is selected"
     (is (nil? (qh/cancel-facet-selection-button)))
     (eh/click-text "Facet 1")
     (is (not (nil? (qh/cancel-facet-selection-button))))
     (eh/click (qh/cancel-facet-selection-button))
     (is (nil? (qh/cancel-facet-selection-button))))

   (testing "selecting a facet fetches the codelists"
     (eh/click-text "Facet 1")
     (is (not (nil? (qh/query-text "Codelists"))))
     (is (= ["Another codelist" "Codelist 1 Label"] (qh/all-labels)))

     (eh/click-text "Facet 2")
     (is (= ["Codelist 2 Label" "Codelist 3 Label"] (qh/all-labels))))

   (testing "codelists are not selected by default"
     (is (= [] (qh/all-selected-labels))))

   (testing "codelists are cached"
     (is (= "Facet 2" @setup/codelist-request))
     (eh/click-text "Facet 1")
     (is (= ["Another codelist" "Codelist 1 Label"] (qh/all-labels)))
     (is (= "Facet 2" @setup/codelist-request)))

   (testing "cancelling facet selection"
     (testing "works for a facet with codelists"
       (is (seq (qh/all-labels)))
       (eh/click (qh/cancel-facet-selection-button))
       (is (empty? (qh/all-labels))))

     (testing "works for a facet with no codelists"
       (eh/click-text "Facet 5")
       (is (not (nil? (qh/query-text "No codelists for facet"))))
       (eh/click (qh/cancel-facet-selection-button))
       (is (nil? (qh/query-text "No codelists for facet"))))))

  (setup/cleanup!))

(deftest expanding-and-collapsing
  (rft/run-test-sync

   (setup/stub-side-effects {:codelists codelists :concept-trees concept-trees})
   (setup/init! facets/configured-facets initial-state)

   (eh/click-text "Facet 2")

   (testing "expanding a codelist fetches its concept tree and expands all children"
     (eh/click (qh/find-expansion-toggle "Codelist 2 Label"))
     (is (= ["Codelist 2 Label" "2-1 child 1" "2-1 child 2"]
            (qh/expanded-labels-under-label "Codelist 2 Label"))))

   (testing "concept trees are cached"
     (is (= "cl2" @setup/concept-tree-request))

     ;; expand codelist 3
     (eh/click (qh/find-expansion-toggle "Codelist 3 Label"))
     (is (= "cl3" @setup/concept-tree-request))

     ;; collapse and re-expand codelist 2
     (eh/click (qh/find-expansion-toggle "Codelist 2 Label"))
     (eh/click (qh/find-expansion-toggle "Codelist 2 Label"))

     ;; codelist 2 tree is not re-fetched
     (is (= "cl3" @setup/concept-tree-request)))

   (testing "expanding and collapsing a sub-tree works"
     (eh/click (qh/find-expansion-toggle "2-1 child 2"))
     (is (= ["Codelist 2 Label" "2-1 child 1" "2-1 child 2" "2-2 child 1" "2-2 child 2"]
            (qh/expanded-labels-under-label "Codelist 2 Label")))

     (eh/click (qh/find-expansion-toggle "2-1 child 2"))
     (is (= ["Codelist 2 Label" "2-1 child 1" "2-1 child 2"]
            (qh/expanded-labels-under-label "Codelist 2 Label"))))

   (testing "expanding a parent node expands only immediate children"
     (eh/click (qh/find-expansion-toggle "Codelist 2 Label"))
     (is (= ["Codelist 2 Label"] (qh/expanded-labels-under-label "Codelist 2 Label")))
     (eh/click (qh/find-expansion-toggle "Codelist 2 Label"))
     (is (= ["Codelist 2 Label" "2-1 child 1" "2-1 child 2"]
            (qh/expanded-labels-under-label "Codelist 2 Label"))))

   (testing "codelists remain sorted by uri when expanded/collapsed"
     (eh/click-text "Facet 1")
     (is (= ["Another codelist" "Codelist 1 Label"] (qh/all-labels)))
     (eh/click (qh/find-expansion-toggle "Codelist 1 Label"))
     (is (= ["Another codelist" "Codelist 1 Label"] (qh/all-labels))))

   (testing "a message is shown when a codelist has no codes"
     (eh/click-text "Facet 3")
     (eh/click (qh/find-expansion-toggle "Codelist no codes"))
     (is (not (nil? (qh/query-text "No codes to show"))))
     (is (qh/open? (qh/find-expansion-toggle "Codelist no codes"))))

   (testing "expansion toggle still toggles when codelist is empty"
     (eh/click (qh/find-expansion-toggle "Codelist no codes"))
     (is (qh/closed? (qh/find-expansion-toggle "Codelist no codes")))))

  (setup/cleanup!))

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
       (is (= ["Codelist 2 Label" "Codelist 3 Label"] (qh/all-labels)))

       (eh/click (qh/find-expansion-toggle "Codelist 3 Label"))
       (is (= ["Codelist 2 Label" "Codelist 3 Label" "3-1 child 1"] (qh/all-labels)))))

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

   (testing "all codelists have an 'any' button"
     (is (not (nil? (qh/select-any-button "Codelist 2 Label"))))
     (is (not (nil? (qh/select-any-button "Codelist 3 Label")))))

   (testing "selecting any"
     (testing "collapses children"
       (eh/click-text "Codelist 2 Label")
       (is (= ["Codelist 2 Label"] (qh/all-selected-labels)))
       (is (= 5 (count (qh/expanded-labels-under-label "Codelist 2 Label"))))

       (eh/click (qh/select-any-button "Codelist 2 Label"))
       (is (= 1 (count (qh/expanded-labels-under-label "Codelist 2 Label")))))

     (testing "selects parent and un-selects all children"
       (eh/click (qh/find-expansion-toggle "Codelist 2 Label"))
       (is (= 1 (count (qh/all-selected-labels))))

       (eh/click-text "Codelist 2 Label")
       (eh/click-text "2-1 child 1")
       (eh/click-text "2-2 child 1")
       (is (= 2 (count (qh/all-selected-labels))))

       (eh/click (qh/select-any-button "Codelist 2 Label"))
       (eh/click (qh/find-expansion-toggle "Codelist 2 Label"))
       (is (= 1 (count (qh/all-selected-labels))))))

   (testing "checking any child unselects the parent codelist"
     (eh/click-text "2-1 child 2")
     (is (= ["2-1 child 2"] (qh/all-selected-labels))))

   (testing "unused codes are not selectable"
     (eh/click-text "2-1 child 2")
     (is (= [] (qh/all-selected-labels)))
     (eh/click-text "2-2 child 2")
     (is (= [] (qh/all-selected-labels))))

   (testing "selecting 'all children' or 'none'"
     (testing "selects all the children that are used"
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
