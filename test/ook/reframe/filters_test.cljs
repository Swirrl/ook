(ns ook.reframe.filters-test
  (:require
   [cljs.test :refer-macros [deftest is]]
   [day8.re-frame.test :as rft]
   [re-frame.core :as rf]
   [ook.reframe.events]
   [ook.reframe.subs]))

(def facet1 {:name "facet 1"
             :dimensions ["dim1" "dim2"]
             :codelists [{:ook/uri "cl1" :label "codelist 1"}]})

(def facet2 {:name "facet 2"
             :dimensions ["dim3" "dim4"]
             :codelists [{:ook/uri "cl2" :label "codelist 2"}
                         {:ook/uri "cl3" :label "codelist 3"}]})
(def facets [facet1 facet2])

(deftest setting-current-facet
  (rft/run-test-sync
   (rf/dispatch [:init/initialize-db {:facets facets :dataset-count 10}])

   (let [current-facet (rf/subscribe [:ui.facets/current])]
     (is nil? @current-facet)

     (rf/dispatch [:ui.facets/set-current facet1])
     (is (= (assoc facet1 :selection #{"cl1"}) @current-facet)))))
