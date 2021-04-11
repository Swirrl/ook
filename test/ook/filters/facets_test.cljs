(ns ook.filters.facets-test
  (:require
   [cljs.test :refer-macros [deftest testing is async]]
   [re-frame.core :as rf]
   [day8.re-frame.test :as rft]
   [ook.test.util.setup :as setup]

   [ook.test.util.event-helpers :as eh]
   [ook.test.util.query-helpers :as qh]
   [ook.reframe.views.filters :as filters]

   [ook.reframe.events :as events]
   [ook.reframe.events.filter-ui]
   [ook.reframe.subs]))

(def initial-state
  {:facets [{:name "Facet 1" :dimensions ["dim1" "dim2"]}
            {:name "Facet 2" :dimensions ["dim3"]}]
   :dataset-count 20})

(def codelists
  {"Facet 1" [{:ook/uri "cl1" :label "Codelist 1 Label"}]
   "Facet 2" [{:ook/uri "cl2" :label "Codelist 2 Label"}
              {:ook/uri "cl3" :label "Codelist 3 Label"}]})

(def concept-trees
  {"cl2" [{:scheme "cl2" :ook/uri "cl2-code1" :label "1 child 1" :children nil}
          {:scheme "cl2" :ook/uri "cl2-code2" :label "1 child 2"
           :children [{:scheme "cl2" :ook/uri "cl2-code3" :label "2 child 1" :children nil}
                      {:scheme "cl2" :ook/uri "cl2-code4" :label "2 child 2" :children nil}]}]})

(def codelist-request (atom nil))

(defn stub-codelist-fetch-success []
  (rf/reg-event-fx
   :facets.codelists/fetch-codelists
   [events/validation-interceptor]
   (fn [_ [_ {:keys [name]}]]
     (reset! codelist-request name)
     {:dispatch [:facets.codelists/success name (get codelists name)]})))

(defn stub-code-fetch-success []
  (rf/reg-event-fx
   :facets.codes/fetch-codes
   [events/validation-interceptor]
   (fn [_ [_ {:keys [ook/uri] :as codelist}]]
     {:dispatch [:facets.codes/success codelist (get concept-trees uri)]})))

(defn select-any-buttons []
  (qh/find-all-text "any"))

(deftest selecting-facets
  (rft/run-test-sync
   (stub-codelist-fetch-success)
   (setup/init! filters/configured-facets initial-state)

   (testing "selecting a facet fetches the codelists"
     (eh/click-text "Facet 1")
     (is (not (nil? (qh/find-text "Codelists"))))
     (is (= ["Codelist 1 Label"] (qh/all-labels)))

     (eh/click-text "Facet 2")
     (is (= ["Codelist 2 Label" "Codelist 3 Label"] (qh/all-labels))))

   (testing "codelists are not selected by default"
     (is (= [] (qh/all-selected-labels))))

   (testing "codelists are cached"
     (is (= "Facet 2" @codelist-request))
     (eh/click-text "Facet 1")
     (is (= ["Codelist 1 Label"] (qh/all-labels)))
     (is (= "Facet 2" @codelist-request)))

   (testing "cancelling facet selection works"
     (is (seq (qh/all-labels)))
     (eh/cancel-facet-selection)
     (is (empty? (qh/all-labels)))) )

  (setup/cleanup!))

(deftest expanding-and-collapsing
  (rft/run-test-sync
    (stub-codelist-fetch-success)
    (stub-code-fetch-success)
    (setup/init! filters/configured-facets initial-state)
    (eh/click-text "Facet 2")

    (testing "expanding a codelist fetches its concept tree and expands all children"
      (eh/click-expansion-toggle "Codelist 2 Label")
      (is (= ["Codelist 2 Label" "1 child 1" "1 child 2" "2 child 1" "2 child 2"]
             (qh/expanded-labels-under-label "Codelist 2 Label"))))

    (testing "collapsing a sub-tree works"
      (eh/click-expansion-toggle "1 child 2")
      (is (= ["Codelist 2 Label" "1 child 1" "1 child 2"]
             (qh/expanded-labels-under-label "Codelist 2 Label"))))

    (testing "expanding a parent node expands all its children"
      (eh/click-expansion-toggle "Codelist 2 Label")
      (is (= ["Codelist 2 Label"] (qh/expanded-labels-under-label "Codelist 2 Label")))
      (eh/click-expansion-toggle "Codelist 2 Label")
      (is (= ["Codelist 2 Label" "1 child 1" "1 child 2" "2 child 1" "2 child 2"]
             (qh/expanded-labels-under-label "Codelist 2 Label")))))

  (setup/cleanup!))

(deftest selection
  (rft/run-test-sync
   (stub-codelist-fetch-success)
   (stub-code-fetch-success)
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

     (testing "un-selects all children"
       (eh/click-expansion-toggle "Codelist 2 Label")
       (is (= 1 (count (qh/all-selected-labels))))

       (eh/click-select-toggle "Codelist 2 Label")
       (eh/click-select-toggle "1 child 1")
       (eh/click-select-toggle "2 child 1")
       (is (= 2 (count (qh/all-selected-labels))))

       (eh/click-select-any "Codelist 2 Label")
       (eh/click-expansion-toggle "Codelist 2 Label")
       (is (= 1 (count (qh/all-selected-labels))))))

   (testing "checking any child then unselects the parent"
     (eh/click-select-toggle "1 child 2")
     (is (= ["1 child 2"] (qh/all-selected-labels)))

     (eh/click-select-any "Codelist 2 Label")
     (eh/click-expansion-toggle "Codelist 2 Label")


     ;; (eh/click-select)
     )

   (testing "selecting 'all children'"
     (testing "selects all the children"
       )

     (testing "unselects the parent (if it was selected)")
     ;;
     )



   ;; (testing "expanding a codelist fetches its concept tree")
   )

  ;; (setup/cleanup!)
  )

;; (deftest selecting-codelists
;;   (rft/run-test-sync
;;    (stub-codelist-fetch-success)
;;    (stub-code-fetch-success)
;;    (setup/init! filters/configured-facets initial-state)

;;    (testing "all codelists have 'select any' button"
;;      (eh/click-text "Facet 2")
;;      (is (= 2 (count (select-any-buttons)))))

;;    (testing "selecting 'any' checks the parent but no children"
;;      (eh/click-text "Codelist 2 Label")
;;      ;; (is (expanded? "Codelist 2 Label"))
;;      )

;;    (testing "expanding a codelist fetches its concept tree"))

;;   (setup/cleanup!)
;;   )
