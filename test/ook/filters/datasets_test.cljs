(ns ook.filters.datasets-test
  (:require
   [cljs.test :refer-macros [deftest testing is]]
   [day8.re-frame.test :as rft]
   [ook.test.util.setup :as setup]

   [ook.test.util.event-helpers :as eh]
   [ook.test.util.query-helpers :as qh]
   [ook.reframe.views :as views]
   [ook.params.parse :as p]
   [ook.reframe.router :as router]
   [reagent.core :as r]

   [ook.reframe.events]
   [ook.reframe.subs]))

(def initial-state
  {:facets {"Facet 1" {:name "Facet 1" :sort-priority 1 :dimensions ["dim1" "dim2"]}
            "Facet 2" {:name "Facet 2" :sort-priority 2 :dimensions ["dim3"]}}
   :dataset-count 2})

(def ds1 {:ook/uri "ds1" :label "Dataset 1" :comment "Dataset 1 description"})
(def ds2 {:ook/uri "ds2" :label "Dataset 2" :comment "Dataset 2 description"})
(def initial-datasets [ds1 ds2])

(def codelists
  {"Facet 1" [{:ook/uri "cl1" :label "Codelist 1 Label"}]
   "Facet 2" [{:ook/uri "cl2" :label "Codelist 2 Label"}
              {:ook/uri "cl3" :label "Codelist 3 Label"}]})

(def concept-trees
  {"cl1" [{:scheme "cl1" :ook/uri "cl1-code1" :label "1-1 child 1" :used true}]
   "cl3" [{:scheme "cl3" :ook/uri "cl3-code1" :label "3-1 child 1" :used true}]
   "cl2" [{:scheme "cl2" :ook/uri "cl2-code1" :label "2-1 child 1" :used true}
          {:scheme "cl2" :ook/uri "cl2-code2" :label "2-1 child 2" :used true
           :children [{:scheme "cl2" :ook/uri "cl2-code3" :label "2-2 child 1" :used true}
                      {:scheme "cl2" :ook/uri "cl2-code4" :label "2-2 child 2"}]}]})
(def datasets
  {nil initial-datasets
   {} initial-datasets
   {"Facet 2" {"cl2" nil}} [(assoc ds1
                                   :matching-observation-count 123
                                   :facets [{:name "Facet 2"
                                             :dimensions [{:ook/uri "dim2"
                                                           :label "Dimension 2 Label"
                                                           :codes [{:ook/uri "a-code"
                                                                    :label "Label for code"}]}]}])]
   {"Facet 2" {"cl2" #{"cl2-code3"}}} []})

(deftest filtering-datasets
  (rft/run-test-sync

   (setup/stub-side-effects {:datasets datasets :codelists codelists :concept-trees concept-trees})
   (setup/init! views/search initial-state)

   (testing "apply filter button"
     (testing "does not show if no facet is selected"
       (is (zero? (count (qh/all-labels))))
       (is (nil? (qh/apply-filter-button))))

     (testing "shows when a facet is selected"
       (eh/click-text "Facet 1")

       (is (not (nil? (qh/apply-filter-button)))))

     (testing "is disabled when no codes or codelists are selected"
       (is (zero? (count (qh/all-selected-labels))))
       (is (qh/disabled? (qh/apply-filter-button))))

     (testing "is not disabled when a codelist is selected"
       (eh/click-text "Codelist 1 Label")
       (is (not (qh/disabled? (qh/apply-filter-button)))))

     (testing "is not disabled when a code is selected"
       (eh/click (qh/find-expansion-toggle "Codelist 1 Label"))
       (is (= ["Codelist 1 Label" "1-1 child 1"] (qh/all-labels)))
       (eh/click-text "1-1 child 1")
       (is (not (qh/disabled? (qh/apply-filter-button))))))

   (testing "applying a facet"
     (eh/click-text "1-1 child 1")
     (eh/click-text "Facet 2")
     (eh/click-text "Codelist 2 Label")
     (eh/click (qh/apply-filter-button))

     (testing "fetches datasets"
       (is (= "Found 1 dataset covering 123 observations" (qh/dataset-count-text)))
       (is (= ["Dataset 1"] (qh/all-dataset-titles))))

     (testing "adds current facet to results table"
       (is (= ["Publisher / Title / Description" "Facet 2" ""] (qh/datset-results-columns))))

     (testing "shows matching codes in the results table"
       (is (= ["Dimension 2 LabelLabel for code"] (qh/column-x-contents 2))))

     (testing "marks applied facet as editable"
       (is (= ["Facet 1" "Facet 2"] (qh/all-available-facets)))
       (is (not (nil? (qh/editable-facet-button "Facet 2")))))

     (testing "removes current facet chrome from ui"
       (is (nil? (qh/apply-filter-button)))
       (is (nil? (qh/query-text "Codelists")))))

   (testing "removing a facet"
     (testing "removes current facet chrome from ui"
       (eh/click (qh/editable-facet-button "Facet 2"))
       (eh/click (qh/remove-facet-button "Facet 2"))

       (is (nil? (qh/apply-filter-button)))
       (is (nil? (qh/query-text "Codelists"))))

     (testing "resets the dataset list"
       (is (= "Showing all datasets" (qh/dataset-count-text)))
       (is (= ["Dataset 1" "Dataset 2"] (qh/all-dataset-titles))))

     (testing "removes the column from the results table"
       (is (= ["Publisher / Title / Description"] (qh/datset-results-columns))))

     (testing "re-adds the facet to the list of possible ones to choose from"
       (is (= ["Facet 1" "Facet 2"] (qh/all-available-facets)))))

   (testing "switching facets applies the current facet, if there is one"
     (eh/click-text "Facet 2")
     (eh/click-text "Codelist 2 Label")

     (eh/click-text "Facet 1")

     (is (= "Found 1 dataset covering 123 observations" (qh/dataset-count-text)))
     (is (= ["Dataset 1"] (qh/all-dataset-titles))))

   (testing "clearing all filters"
     (eh/click-text "Facet 1")
     (eh/click-text "Codelist 1 Label")
     (eh/click (qh/apply-filter-button))

     (is (= "No datasets matched the applied filters. Clear filters to reset and make a new selection."
            (qh/dataset-count-text)))

     (eh/click-text "Clear filters")

     (testing "navigates home"
       (is (= [:ook.route/home nil] @setup/last-navigation)))

     (testing "resets dataset list"
       (is (= ["Dataset 1" "Dataset 2"] (qh/all-dataset-titles)))))

   (setup/cleanup!)))

(deftest editing-facets
  (rft/run-test-sync
   (setup/stub-side-effects {:datasets datasets :codelists codelists :concept-trees concept-trees})
   (setup/init! views/search initial-state)

   (testing "fetches codelists if they're not already cached (i.e. visiting from a permalink with filters)"
     (router/search-controller {:query {:filters (p/serialize-filter-state {"Facet 2" {"cl2" nil}})}})
     (r/flush)
     (is (= "Found 1 dataset covering 123 observations" (qh/dataset-count-text)))
     (is (nil? @setup/codelist-request))

     (eh/click (qh/editable-facet-button "Facet 2"))
     (is (= "Facet 2" @setup/codelist-request)))

   (testing "it sets the previous selection properly"
     (is (= ["Codelist 2 Label"] (qh/all-selected-labels))))

   (testing "it sets the disclosure properly given the selection"
     (is (qh/closed? (qh/find-expansion-toggle "Codelist 2 Label"))))

   (testing "fetches concept trees for selected codes if they're used and not already cached"
     (eh/click (qh/cancel-facet-selection-button))
     (is (nil? @setup/concept-tree-request))
     (router/search-controller
      {:query {:filters (p/serialize-filter-state {"Facet 2" {"cl2" #{"cl2-code3"}}})}})
     (r/flush)

     (is (= "No datasets matched the applied filters. Clear filters to reset and make a new selection."
            (qh/dataset-count-text)))

     (eh/click (qh/editable-facet-button "Facet 2"))
     (is (= "cl2" @setup/concept-tree-request)))

   (testing "it sets the selection properly"
     (is (= ["2-2 child 1"] (qh/all-selected-labels))))

   (testing "it sets the disclosure properly for the selection"
     (is (qh/open? (qh/find-expansion-toggle "Codelist 2 Label")))
     (is (qh/open? (qh/find-expansion-toggle "2-1 child 2")))
     (is (qh/closed? (qh/find-expansion-toggle "Codelist 3 Label"))))

   (testing "updating the selection and reapplying the facet works"
     (eh/click-text "2-2 child 1")
     (eh/click-text "2-1 child 1")

     (eh/click (qh/apply-filter-button))
     (is (= {"Facet 2" {"cl2" #{"cl2-code1"}}} @setup/dataset-request))
     (is (= "No datasets matched the applied filters. Clear filters to reset and make a new selection."
            (qh/dataset-count-text))))

   (testing "it shows the right selection and disclosure"
     (eh/click (qh/editable-facet-button "Facet 2"))
     ;; (is (= ["Codelist 2 Label" "2-1 child 1" "2-1 child 2" "Codelist 3 Label"] (qh/all-labels)))
     (is (= ["Codelist 2 Label" "2-1 child 1" "2-1 child 2" "2-2 child 1" "2-2 child 2" "Codelist 3 Label"]
            (qh/all-labels)))
     (is (= ["2-1 child 1"] (qh/all-selected-labels))))))
