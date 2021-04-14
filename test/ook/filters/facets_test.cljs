(ns ook.filters.facets-test
  (:require
   [cljs.test :refer-macros [deftest testing is]]
   [day8.re-frame.test :as rft]
   [ook.test.util.setup :as setup]

   [ook.test.util.event-helpers :as eh]
   [ook.test.util.query-helpers :as qh]
   [ook.reframe.views.filters :as filters]

   [ook.reframe.events]
   [ook.reframe.events.filter-ui]
   [ook.reframe.subs]))

(def initial-state
  {:facets [{:name "Facet 1" :dimensions ["dim1" "dim2"]}
            {:name "Facet 2" :dimensions ["dim3"]}]
   :dataset-count 20})

(def codelists
  {"Facet 1" [{:ook/uri "cl1" :label "Codelist 1 Label"}
              {:ook/uri "another-codelist" :label "Another codelist"}]
   "Facet 2" [{:ook/uri "cl2" :label "Codelist 2 Label"}
              {:ook/uri "cl3" :label "Codelist 3 Label"}]})

(def concept-trees
  {"cl3" [{:scheme "cl3" :ook/uri "cl3-code1" :label "3-1 child 1"}]
   "cl2" [{:scheme "cl2" :ook/uri "cl2-code1" :label "2-1 child 1" :children nil}
          {:scheme "cl2" :ook/uri "cl2-code2" :label "2-1 child 2"
           :children [{:scheme "cl2" :ook/uri "cl2-code3" :label "2-2 child 1" :children nil}
                      {:scheme "cl2" :ook/uri "cl2-code4" :label "2-2 child 2" :children nil}]}]})

(deftest selecting-facets
  (rft/run-test-sync
   (setup/stub-codelist-fetch-success codelists)
   (setup/init! filters/configured-facets initial-state)

   (testing "clear facet selection button only shows when a facet is selected"
     (is (nil? (qh/cancel-facet-selection-button)))
     (eh/click-text "Facet 1")
     (is (not (nil? (qh/cancel-facet-selection-button))))
     (eh/cancel-facet-selection)
     (is (nil? (qh/cancel-facet-selection-button))))

   (testing "selecting a facet fetches the codelists"
     (eh/click-text "Facet 1")
     (is (not (nil? (qh/find-text "Codelists"))))
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

   (testing "cancelling facet selection works"
     (is (seq (qh/all-labels)))
     (eh/cancel-facet-selection)
     (is (empty? (qh/all-labels)))))

  (setup/cleanup!))

(deftest expanding-and-collapsing
  (rft/run-test-sync
   (setup/stub-codelist-fetch-success codelists)
   (setup/stub-code-fetch-success concept-trees)
   (setup/init! filters/configured-facets initial-state)
   (eh/click-text "Facet 2")

   (testing "expanding a codelist fetches its concept tree and expands all children"
     (eh/click-expansion-toggle "Codelist 2 Label")
     (is (= ["Codelist 2 Label" "2-1 child 1" "2-1 child 2" "2-2 child 1" "2-2 child 2"]
            (qh/expanded-labels-under-label "Codelist 2 Label"))))

   (testing "concept trees are cached"
     (is (= "Codelist 2 Label" @setup/concept-tree-request))

     ;; expand codelist 3
     (eh/click-expansion-toggle "Codelist 3 Label")
     (is (= "Codelist 3 Label" @setup/concept-tree-request))

     ;; collapse and re-expand codelist 2
     (eh/click-expansion-toggle "Codelist 2 Label")
     (eh/click-expansion-toggle "Codelist 2 Label")

     ;; codelist 2 tree is not re-fetched
     (is (= "Codelist 3 Label" @setup/concept-tree-request)))

   (testing "collapsing a sub-tree works"
     (eh/click-expansion-toggle "2-1 child 2")
     (is (= ["Codelist 2 Label" "2-1 child 1" "2-1 child 2"]
            (qh/expanded-labels-under-label "Codelist 2 Label"))))

   (testing "expanding a parent node expands all its children"
     (eh/click-expansion-toggle "Codelist 2 Label")
     (is (= ["Codelist 2 Label"] (qh/expanded-labels-under-label "Codelist 2 Label")))
     (eh/click-expansion-toggle "Codelist 2 Label")
     (is (= ["Codelist 2 Label" "2-1 child 1" "2-1 child 2" "2-2 child 1" "2-2 child 2"]
            (qh/expanded-labels-under-label "Codelist 2 Label"))))

   (testing "codelists remain sorted by uri when expanded/collapsed"
     (eh/click-text "Facet 1")
     (is (= ["Another codelist" "Codelist 1 Label"] (qh/all-labels)))
     (eh/click-expansion-toggle "Codelist 1 Label")
     (is (= ["Another codelist" "Codelist 1 Label"] (qh/all-labels)))))

  (setup/cleanup!))

(deftest selection
  (rft/run-test-sync
   (setup/stub-codelist-fetch-success codelists)
   (setup/stub-code-fetch-success concept-trees)
   (setup/init! filters/configured-facets initial-state)
   (eh/click-text "Facet 2")

   (testing "fetching codes does not change selection"
     (eh/click-select-toggle "Codelist 2 Label")
     (is (= 1 (count (qh/all-selected-labels))))

     (eh/click-expansion-toggle "Codelist 2 Label")
     (is (= 1 (count (qh/all-selected-labels)))))

   (testing "all codelists have an 'any' button"
     (is (not (nil? (qh/select-any-button "Codelist 2 Label"))))
     (is (not (nil? (qh/select-any-button "Codelist 3 Label")))))

   (testing "selecting any"
     (testing "collapses children"
       (is (= ["Codelist 2 Label"] (qh/all-selected-labels)))
       (is (= 5 (count (qh/expanded-labels-under-label "Codelist 2 Label"))))

       (eh/click-select-any "Codelist 2 Label")
       (is (= 1 (count (qh/expanded-labels-under-label "Codelist 2 Label")))))

     (testing "selects parent and un-selects all children"
       (eh/click-expansion-toggle "Codelist 2 Label")
       (is (= 1 (count (qh/all-selected-labels))))

       (eh/click-select-toggle "Codelist 2 Label")
       (eh/click-select-toggle "2-1 child 1")
       (eh/click-select-toggle "2-2 child 1")
       (is (= 2 (count (qh/all-selected-labels))))

       (eh/click-select-any "Codelist 2 Label")
       (eh/click-expansion-toggle "Codelist 2 Label")
       (is (= 1 (count (qh/all-selected-labels))))))

   (testing "checking any child unselects the parent codelist"
     (eh/click-select-toggle "2-1 child 2")
     (is (= ["2-1 child 2"] (qh/all-selected-labels))))

   (testing "selecting 'all children'"
     (testing "selects all the children"
       (is (= ["2-1 child 2"] (qh/all-selected-labels)))
       (eh/click-select-all-children "2-1 child 2")
       (is (= ["2-1 child 2" "2-2 child 1" "2-2 child 2"] (qh/all-selected-labels))))

     (testing "unselects parent codelist if it was selected"
       (eh/click-select-toggle "Codelist 2 Label")

       (is (= ["Codelist 2 Label"] (qh/all-selected-labels)))
       (eh/click-select-all-children "2-1 child 2")
       (is (= ["2-2 child 1" "2-2 child 2"] (qh/all-selected-labels))))))

  (setup/cleanup!))
