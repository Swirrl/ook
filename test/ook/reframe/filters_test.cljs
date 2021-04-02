(ns ook.reframe.filters-test
  (:require
   [cljs.test :refer-macros [deftest testing is]]
   [day8.re-frame.test :as rft]
   [re-frame.core :as rf]
   [ook.reframe.events :as events]
   [ook.reframe.subs]))

(def codelist1 {:ook/uri "cl1" :label "codelist 1"})
(def codelist2 {:ook/uri "cl2" :label "codelist 2"})
(def codelist3 {:ook/uri "cl3" :label "codelist 3"})
(def codelist4 {:ook/uri "cl4" :label "codelist 4"})

(def facet1 {:name "facet1"
             :dimensions [{:ook/uri "dim1" :codelists [codelist1]}]
             :codelists [codelist1]})

(def facet2 {:name "facet2"
             :dimensions [{:ook/uri "dim2" :codelists [codelist2]}
                          {:ook/uri "dim3" :codelists [codelist3 codelist4]}]
             :codelists [codelist2 codelist3]})
(def facets [facet1 facet2])

;; stub actual http call

(def dataset1 {:ook/uri "dataset1" :label "Dataset label"})

(rf/reg-event-db
 :datasets/fetch-datasets
 [events/validation-interceptor]
 (fn [db [_ facets]]
   (cond-> db
     (= facets ["facet1,cl1"]) (assoc :results.datasets/data [(assoc dataset1 :facets [facet1])])
     (= facets []) (assoc :results.datasets/data []))))

(deftest selecting-and-applying-facets
  (rft/run-test-sync
   (rf/dispatch [:init/initialize-db {:facets facets :dataset-count 10}])

   (testing "setting and resetting current facet works"
     (let [current-facet (rf/subscribe [:ui.facets/current])]
       (is nil? @current-facet)

       (rf/dispatch [:ui.facets/set-current facet1])
       (is (= (assoc facet1 :selection #{"cl1"}) @current-facet))

       (rf/dispatch [:ui.facets/set-current facet2])
       (is (= (assoc facet2 :selection #{"cl2" "cl3"}) @current-facet))

       (rf/dispatch [:ui.facets/cancel-current-selection])
       (is nil? @current-facet)))

   (testing "applying current facet works"
     (let [datasets (rf/subscribe [:results.datasets/data])]
       (is nil? datasets)

       (rf/dispatch [:filters/apply ["facet1,cl1"]])
       (is (= [(assoc dataset1 :facets [facet1])] @datasets))

       (rf/dispatch [:filters/apply []])
       (is (= [] @datasets))))))
