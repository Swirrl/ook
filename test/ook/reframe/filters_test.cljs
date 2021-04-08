(ns ook.reframe.filters-test
  (:require
   [cljs.test :refer-macros [deftest testing is]]
   [day8.re-frame.test :as rft]
   [re-frame.core :as rf]
   [ook.test.util.simulate-user :as sim]
   [ook.reframe.events :as events]
   [ook.reframe.events.filter-ui]
   [ook.reframe.subs]))

(def codelist1 {:ook/uri "cl1" :label "codelist 1"})
(def codelist2 {:ook/uri "cl2" :label "codelist 2"})
(def codelist3 {:ook/uri "cl3" :label "codelist 3"})
(def codelist4 {:ook/uri "cl4" :label "codelist 4"})

(def facet1 {:name "facet1" :dimensions ["dim1"]})
(def facet2 {:name "facet2" :dimensions ["dim2" "dim3"]})
(def facets [facet1 facet2])

(def code1 {:ook/uri "code1" :label "code 1"})
(def code3 {:ook/uri "code3" :label "code 3"})
(def code4 {:ook/uri "code4" :label "code 4"})
(def code-with-children {:ook/uri "code2" :label "code 2" :children [code3 code4]})

;; Stub HTTP calls to call success handlers directly

;; (def dataset1 {:ook/uri "dataset1" :label "Dataset label"})

;; (rf/reg-event-db
;;  :datasets/fetch-datasets
;;  [events/validation-interceptor]
;;  (fn [db [_ facets]]
;;    (cond-> db
;;      (= facets ["facet1,cl1"]) (assoc :results.datasets/data [(assoc dataset1 :facets [facet1])])
;;      (= facets []) (assoc :results.datasets/data []))))

;; Assuming dim1 goes with codelist 1 and codelist 2

(def facet1-codelists
  [codelist1 codelist2])

(def facet1-with-codelists
  (assoc facet1 :tree (map #(assoc % :children [] :allow-any? true) facet1-codelists)))

(def codelist-request (atom nil))

(rf/reg-sub
  :app/db
  (fn [db _] db))

(rf/reg-event-fx
 :facets.codelists/fetch-codelists
 [events/validation-interceptor]
 (fn [_ [_ {:keys [name]}]]
   (reset! codelist-request name)
   {:dispatch [:facets.codelists/success "facet1" facet1-codelists]}))

(rf/reg-event-fx
  :facets.codes/fetch-codes
  [events/validation-interceptor]
  (fn [_ [_ {:keys [ook/uri] :as codelist}]]
    {:dispatch [:facets.codes/success codelist [code1 code-with-children]]}))

(deftest selecting-and-applying-facets
  (rft/run-test-sync
   (rf/dispatch [:init/initialize-db {:facets facets :dataset-count 10}])

   (let [current-facet (rf/subscribe [:ui.facets/current])
         db (rf/subscribe [:app/db])]
     (testing "setting and resetting current facet works"
       (is nil? @current-facet)

       (sim/click-facet facet1)
       (is (= (assoc facet1-with-codelists
                     :selection #{}
                     :expanded #{})
              @current-facet))
       (is (= "facet1" @codelist-request))

       (sim/cancel-current-selection)
       (is nil? @current-facet))

     (testing "it caches codelist results"
       (is (= facet1-with-codelists
              (->> @db :facets/config (filter #(= (:name %) "facet1")) first)))

       (sim/click-facet facet2)
       (sim/click-facet facet1-with-codelists)
       (is (= "facet1" (:name @current-facet)))
       (is (= "facet2" @codelist-request)))

     ;; (testing "expanding codelist works"
     ;;   (sim/expand-codelist codelist1)
     ;;   )

   ;; (testing "applying current facet works"
   ;;   (let [datasets (rf/subscribe [:results.datasets/data])]
   ;;     (is nil? datasets)

   ;;     (rf/dispatch [:filters/apply ["facet1,cl1"]])
   ;;     (is (= [(assoc dataset1 :facets [facet1])] @datasets))

   ;;     (rf/dispatch [:filters/apply []])
   ;;     (is (= [] @datasets))))
     )))
