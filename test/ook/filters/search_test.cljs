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
            "Facet 2" {:name "Facet 2" :sort-priority 2 :dimensions ["dim3"]}}
   :dataset-count 20})

(def codelists
  {"Facet 1" [{:ook/uri "cl1" :label "Codelist 1 Label"}
              {:ook/uri "another-codelist" :label "Another codelist"}]
   "Facet 2" [{:ook/uri "cl2" :label "Codelist 2 Label"}
              {:ook/uri "cl3" :label "Codelist 3 Label"}]})

(def concept-trees
  {"cl3" [{:scheme "cl3" :ook/uri "cl3-code1" :label "3-1 child 1" :used true}]
   "cl2" [{:scheme "cl2" :ook/uri "cl2-code1" :label "2-1 child 1" :used true :children nil}
          {:scheme "cl2" :ook/uri "cl2-code2" :label "2-1 child 2" :used true
           :children [{:scheme "cl2" :ook/uri "cl2-code3" :label "2-2 child 1" :used true}
                      {:scheme "cl2" :ook/uri "cl2-code4" :label "2-2 child 2" :used false}]}]})

(def search-results
  {"2-2 child 1" [{:scheme "cl2" :ook/uri "cl2-code3" :label "2-2 child 1"}]})

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
     (is (nil? (qh/query-text "select all matches")))
     (is (nil? (qh/query-text "reset search"))))

   (testing "searching for a code with no matches"
     (is (= ["Codelist 2 Label" "Codelist 3 Label"] (qh/all-labels)))
     (search-for "no matches!")

     (testing "shows a relevant message and reset button, no select all button"
       (is (nil? (qh/query-text "select all matches")))
       (is (not (nil? (qh/query-text "reset search"))))
       (is (not (nil? (qh/query-text "No codes match"))))
       (is (= [] (qh/all-labels))))

     (testing "can be reset to show all the codelists again"
       (eh/click-text "reset search")

       (is (nil? (qh/query-text "No codes match")))
       (is (= "" (qh/search-input-val)))
       (is (= ["Codelist 2 Label" "Codelist 3 Label"] (qh/all-labels)))))

   (testing "searching for a code by label that matches"
     (is (nil? @setup/concept-tree-request))
     (search-for "2-2 child 1")

     (testing "shows select all and reset options"
       (is (not (nil? (qh/query-text "select all matches"))))
       (is (not (nil? (qh/query-text "reset search")))))

     ;; (testing "expands the right levels of the tree"
     ;;   (is (qh/open? (qh/find-expansion-toggle "Codelist 2 Label")))
     ;;   (is (qh/closed? (qh/find-expansion-toggle "2-1 child 2")))
     ;;   (is (qh/closed? (qh/find-expansion-toggle "Codelist 3 Label"))))

     (testing "fetches code trees that were not already cached"
       (is (= "cl2" @setup/concept-tree-request)))

     (testing "shows only matching codes with all parents expanded"
       (is (= ["Codelist 2 Label" "2-1 child 1"] (qh/all-labels)))

       ;; search for a deeper nested code
       )

     (testing "can be reset to show all the codelists again"))

   (testing "select all matches"
     ;; select something
     ;; do a search that excludes that thing
     ;; select all matches
     ;; selection is only the matching results (no the old one, too)
     )

   (testing "interactions with previous selection and disclosure"
     (testing "selection is unaffected by search"
       ;; select something
       ;; search for the selected thing
       ;; it should not be selected


       ;; select something
       ;; search for something else
       ;; clear the search
       ;; the thing should no longer be selected
       )
     (testing "it cancels any previous disclosure"
       ;; expand something
       ;; search
       ;; clear the search
       ;; expansion should be reset (all closed)
       )))

  (setup/cleanup!))
