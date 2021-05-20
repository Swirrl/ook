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
            "Facet 5" {:name "Facet 5" :sort-priority 4 :dimensions ["dim5"]}}
   :dataset-count 20})

(def codelists
  {"Facet 1" [{:ook/uri "cl1" :label "Codelist 1 Label"}
              {:ook/uri "another-codelist" :label "Another codelist"}]
   "Facet 2" [{:ook/uri "cl2" :label "Codelist 2 Label"}
              {:ook/uri "cl3" :label "Codelist 3 Label"}]
   "Facet 3" [{:ook/uri "no-codes" :label "Codelist no codes"}]
   "Facet 5" []})

(def concept-trees
  {"cl3" [{:scheme "cl3" :ook/uri "cl3-code1" :label "3-1 child 1" :used true}]
   "cl2" [{:scheme "cl2" :ook/uri "cl2-code1" :label "2-1 child 1" :used true :children nil}
          {:scheme "cl2" :ook/uri "cl2-code2" :label "2-1 child 2" :used true
           :children [{:scheme "cl2" :ook/uri "cl2-code3" :label "2-2 child 1" :used true}
                      {:scheme "cl2" :ook/uri "cl2-code4" :label "2-2 child 2" :used false}]}]
   "no-codes" []})

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
     (is (= ["Another codelist" "Codelist 1 Label"] (qh/all-checkbox-labels)))

     (eh/click-text "Facet 2")
     (is (= ["Codelist 2 Label" "Codelist 3 Label"] (qh/all-checkbox-labels))))

   (testing "codelists are not selected by default"
     (is (= [] (qh/all-selected-labels))))

   (testing "codelists are cached"
     (is (= "Facet 2" @setup/codelist-request))
     (eh/click-text "Facet 1")
     (is (= ["Another codelist" "Codelist 1 Label"] (qh/all-checkbox-labels)))
     (is (= "Facet 2" @setup/codelist-request)))

   (testing "cancelling facet selection"
     (testing "works for a facet with codelists"
       (is (seq (qh/all-checkbox-labels)))
       (eh/click (qh/cancel-facet-selection-button))
       (is (empty? (qh/all-checkbox-labels))))

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
     (is (= ["Another codelist" "Codelist 1 Label"] (qh/all-checkbox-labels)))
     (eh/click (qh/find-expansion-toggle "Codelist 1 Label"))
     (is (= ["Another codelist" "Codelist 1 Label"] (qh/all-checkbox-labels))))

   (testing "a message is shown when a codelist has no codes"
     (eh/click-text "Facet 3")
     (eh/click (qh/find-expansion-toggle "Codelist no codes"))
     (is (not (nil? (qh/query-text "No codes to show"))))
     (is (qh/open? (qh/find-expansion-toggle "Codelist no codes"))))

   (testing "expansion toggle still toggles when codelist is empty"
     (eh/click (qh/find-expansion-toggle "Codelist no codes"))
     (is (qh/closed? (qh/find-expansion-toggle "Codelist no codes")))))

  (setup/cleanup!))
