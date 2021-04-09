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

(def codelist-request (atom nil))

(defn stub-code-fetching []
  (rf/reg-event-fx
   :facets.codelists/fetch-codelists
   [events/validation-interceptor]
   (fn [_ [_ {:keys [name]}]]
     (reset! codelist-request name)
     {:dispatch [:facets.codelists/success name (get codelists name)]})))

(defn all-labels []
  (qh/all-text-content ".filters input[type='checkbox'] + label"))

(defn all-selected-labels []
  (qh/all-text-content ".filters input [type='checkbox']:checked + label"))

(defn expand-codelist-button [codelist-name]
  (qh/find-query ))

(defn click-facet [name]
  (eh/click (qh/find-text name)))

(deftest selecting-facets
  (rft/run-test-sync
   (stub-code-fetching)
   (setup/init! filters/configured-facets initial-state)

   (testing "selecting a facet fetches the codelists"
     (click-facet "Facet 1")
     (is (not (nil? (qh/find-text "Codelists"))))
     (is (= ["Codelist 1 Label"] (all-labels)))

     (click-facet "Facet 2")
     (is (= ["Codelist 2 Label" "Codelist 3 Label"] (all-labels))))

   (testing "codelists are not selected by default"
     (is (= [] (all-selected-labels))))

   (testing "codelists are cached"
     (is (= "Facet 2" @codelist-request))
     (click-facet "Facet 1")
     (is (= ["Codelist 1 Label"] (all-labels)))
     (is (= "Facet 2" @codelist-request)))

   (testing "codelists are all expandable"
     (is (not (nil? (expand-codelist-button "Codelist 2 Label"))))
     (is (not (nil? (expand-codelist-button "Codelist 3 Label"))))))

  (setup/cleanup!))
