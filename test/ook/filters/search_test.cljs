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
  {"2-2 child 1" [{:scheme "cl2" :ook/uri "cl2-code3"}]
   "5-3 child 1" [{:scheme "cl5" :ook/uri "cl5-code5"}]
   "this code is reused" [{:scheme "cl6" :ook/uri "reused-code"}
                          {:scheme "cl7" :ook/uri "reused-code"}]})

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
     (is (nil? (qh/query-text "clear search"))))

   (testing "searching for a code with no matches"
     (is (= ["Codelist 2 Label" "Codelist 3 Label"] (qh/all-labels)))
     (search-for "no matches!")

     (testing "shows a relevant message and reset button, no select all button"
       (is (nil? (qh/query-text "select all matches")))
       (is (not (nil? (qh/query-text "clear search"))))
       (is (not (nil? (qh/query-text "No codes match"))))
       (is (= [] (qh/all-labels))))

     (testing "can be reset to show all the codelists again"
       (eh/click-text "clear search")

       (is (nil? (qh/query-text "No codes match")))
       (is (= "" (qh/search-input-val)))
       (is (= ["Codelist 2 Label" "Codelist 3 Label"] (qh/all-labels)))))

   (testing "searching for a code by label that matches"
     (is (nil? @setup/concept-tree-request))
     (search-for "2-2 child 1")

     (testing "shows select all and reset options"
       (is (not (nil? (qh/query-text "select all matches"))))
       (is (not (nil? (qh/query-text "clear search")))))

     (testing "fetches code trees that were not already cached"
       (is (= "cl2" @setup/concept-tree-request)))

     (testing "shows only matching codes with all parents expanded"
       (is (= ["Codelist 2 Label" "2-1 child 2" "2-2 child 1"] (qh/all-labels)))

       (is (qh/open? (qh/find-expansion-toggle "Codelist 2 Label")))
       (is (qh/open? (qh/find-expansion-toggle "2-1 child 2")))

       (eh/click-text "Facet 3")
       (is (= ["with nested codes"] (qh/all-labels)))
       (search-for "5-3 child 1")

       (is (= "cl5" @setup/concept-tree-request))
       (is (= ["with nested codes" "5-1 child 1" "5-2 child 2" "5-3 child 1"] (qh/all-labels))))

     (testing "can be reset to show all the codelists again"
       (eh/click-text "clear search")
       (is (= ["with nested codes"] (qh/all-labels)))
       (is (qh/closed? (qh/find-expansion-toggle "with nested codes"))))

     (testing "it caches code trees"
       (eh/click-text "Facet 2")
       (eh/click (qh/find-expansion-toggle "Codelist 2 Label"))
       (is (= "cl5" @setup/concept-tree-request))))

   (testing "searching for a code that is in multiple codelists"
     (eh/click-text "Facet 4")
     (search-for "this code is reused")

     (testing "shows the result under both expanded codelists"
       (is (qh/open? (qh/find-expansion-toggle "with shared codes 1")))
       (is (qh/open? (qh/find-expansion-toggle "with shared codes 2")))

       (is (= ["with shared codes 1" "this code is reused" "with shared codes 2" "this code is reused"]
              (qh/all-labels)))))

   (testing "searching when the only thing that matches is a codelist itself"
     (search-for "with shared codes 1")

     (testing "does not include the codelist in search results"
       (is (= [] (qh/all-labels))))))

  (setup/cleanup!))

(deftest code-search-results-selection-and-disclosure
  (rft/run-test-sync
   (setup/stub-side-effects {:codelists codelists
                             :concept-trees concept-trees
                             :search-results search-results})

   (setup/init! facets/configured-facets initial-state)

   (eh/click-text "Facet 2")
   (search-for "2-2 child 1")

   (testing "select all matches"
     ;; (testing "works"
     ;;   (is (= [] (qh/all-selected-labels)))
     ;;   (eh/click-text "select all matches")
     ;;   (is (= ["2-2 child 1"] (qh/all-selected-labels))))

     (testing "does not select unused codes")

     (testing "does not unselect any previously selected options")
     ;; select something
     ;; do a search that excludes that thing
     ;; select all matches
     ;; selection is only the matching results (no the old one, too)
     )

   (testing "interactions with previous selection and disclosure"
     (testing "each new search clears any previous selection and disclosure")

     (testing "clearing the search does not affect selection or disclosure"
       ;; select something
       ;; search for the selected thing
       ;; it should not be selected


       ;; select something
       ;; search for something else
       ;; clear the search
       ;; the thing should no longer be selected
       )

     ;; (testing "it cancels any previous disclosure"
     ;;   ;; expand something
     ;;   ;; search
     ;;   ;; clear the search
     ;;   ;; expansion should be reset (all closed)
     ;;   )
     )

   (setup/cleanup!)))
