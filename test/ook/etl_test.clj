(ns ook.etl-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clj-http.core]
            [vcr-clj.clj-http :as vcr]
            [ook.test.util.setup :as setup :refer [with-system]]
            [ook.index :as idx]
            [ook.etl :as etl]
            [ook.search.db :as db]
            [ook.util :as util]))

(deftest extract-test
  (testing "Extracting a page of RDF from a drafter endpoint"
    (with-system [system ["drafter-client.edn" "idp-beta.edn"]]
      (is (= 28 (count (setup/example-datasets system)))))))

(deftest transform-test
  (testing "Transform triples into json-ld"
    (with-system [system ["drafter-client.edn", "idp-beta.edn"]]
      (let [datasets (setup/example-datasets system)
            frame (slurp (io/resource "etl/dataset-frame.json"))
            jsonld (etl/transform frame datasets)]
        (is (= "Annual mean rainfall with trends actual"
               (-> jsonld (get "@graph") first (get "label"))))))))

(deftest load-test
  (testing "Load json-ld into database"
    (with-system [system ["drafter-client.edn" "idp-beta.edn" "elasticsearch-test.edn"]]
      (let [datasets (setup/example-datasets system)
            frame (slurp (io/resource "etl/dataset-frame.json"))
            jsonld (etl/transform frame datasets)
            result (etl/load-documents system "dataset" jsonld)]
        (is (= false (:errors (first result))))
        (is (= true (get-in (idx/delete-indicies system) [:dataset :acknowledged])))))))

(defn found?
  "Checks to see if the doc has the value for the key.

  You can specify nil values to check for the existence of a
  property in the index mapping as otherwise :missing-from-mapping
  would be returned"
  [doc key value]
  (= value
     (get doc key :missing-from-mapping)))

(deftest dataset-pipeline-test
  (testing "Dataset pipeline schema"
    (with-system [system ["drafter-client.edn"
                          "idp-beta.edn"
                          "elasticsearch-test.edn"
                          "project/fixture/data.edn"]]
      (setup/reset-indicies! system)
      (vcr/with-cassette {:name :dataset-pipeline :recordable? setup/not-localhost?}
        (etl/dataset-pipeline system))

      (let [db (setup/get-db system)
            doc (first (db/all-datasets db))]
        (are [key value] (found? doc key value)
          :ook/uri "data/gss_data/climate-change/met-office-annual-mean-rainfall-with-trends-actual-catalog-entry"
          :label "Annual mean rainfall with trends actual")))))

(deftest component-pipeline-test
  (testing "Component pipeline schema"
    (with-system [system ["drafter-client.edn"
                          "idp-beta.edn"
                          "elasticsearch-test.edn"]]
      (setup/reset-indicies! system)
      (vcr/with-cassette {:name :component-pipeline :recordable? setup/not-localhost?}
        (etl/component-pipeline system))

      (let [db (setup/get-db system)
            doc (first (db/get-components db ["def/trade/property/dimension/alcohol-type"]))]
        (are [key value] (found? doc key value)
          :ook/uri "def/trade/property/dimension/alcohol-type"
          :label "Alcohol Type"
          :codelist {:ook/uri "def/trade/concept-scheme/alcohol-type"
                     :label "Alcohol Type"})))))

(deftest code-pipeline-test
  (testing "Code pipeline schema"
    (with-system [system ["drafter-client.edn"
                          "idp-beta.edn"
                          "elasticsearch-test.edn"
                          "project/fixture/data.edn"]]
      (setup/reset-indicies! system)
      (vcr/with-cassette {:name :code-pipeline :recordable? setup/not-localhost?}
        (etl/code-pipeline system)
        (etl/code-used-pipeline system))

      (let [db (setup/get-db system)
            doc (first (db/get-codes db ["data/gss_data/climate-change/met-office-annual-mean-temp-with-trends-actual#concept/geography/wales"]))]
        (are [key value] (found? doc key value)
          :ook/uri "data/gss_data/climate-change/met-office-annual-mean-temp-with-trends-actual#concept/geography/wales"
          :label "Wales"
          :notation "wales"
          :used "true"
          :narrower nil
          :broader nil
          :topConceptOf "data/gss_data/climate-change/met-office-annual-mean-temp-with-trends-actual#scheme/geography")))))

(defn fake-graph->modified [system]
  (let [fake-graph->modified-call-count (atom 0)
        fake-graph->modified-res (ook.etl/graph->modified system)]
    (fn [_]
      (swap! fake-graph->modified-call-count inc)
      (if (< @fake-graph->modified-call-count 2)
        fake-graph->modified-res
        (assoc fake-graph->modified-res
               (-> fake-graph->modified-res keys sort first)
               "1111-11-11T11:11:11.111Z")))))

(deftest observation-pipeline-test
  (with-system [system ["drafter-client.edn"
                        "idp-beta.edn"
                        "elasticsearch-test.edn"
                        "project/fixture/data.edn"]]
      (setup/reset-indicies! system)
      ;; We start with a blank slate so load everything
      (let [total-observations
            (vcr/with-cassette {:name :observation-pipeline-1
                                :recordable? setup/not-localhost?}
              (etl/observation-pipeline system))]
        (is (pos? total-observations))
        ;; But on the second load observations haven't changed
        (is (zero?
             (vcr/with-cassette {:name :observation-pipeline-2
                                 :recordable? setup/not-localhost?}
               (etl/observation-pipeline system))))
        ;; We can simulate changing one dataset by faking
        ;; ook.etl/graph->modified
        (with-redefs [ook.etl/graph->modified (fake-graph->modified system)]
          (is (< 0
                 (vcr/with-cassette {:name :observation-pipeline-3
                                     :recordable? setup/not-localhost?}
                   (etl/observation-pipeline system))
                 total-observations))))))

;; Just like the real pipeline function, but fails when trying to populate the
;; observation index
(defn fake-pipeline []
  (let [real-pipeline ook.etl/pipeline]
    (fn [& args]
      (when (= "observation" (last args)) (/ 1 0))
      (apply real-pipeline args))))

(deftest observation-pipeline-is-transactional-test
  (with-system [system ["drafter-client.edn"
                        "idp-beta.edn"
                        "elasticsearch-test.edn"
                        "project/fixture/data.edn"]]
      (setup/reset-indicies! system)
      ;; Make a note of how many observations should be loaded on a clean slate
      (let [total-observations
            (vcr/with-cassette {:name :observation-pipeline-is-transactional-1
                                :recordable? setup/not-localhost?}
              (etl/observation-pipeline system))]
        (setup/reset-indicies! system)
        ;; Simulate the pipeline encountering an error half way through
        (try
          (with-redefs [ook.etl/pipeline (fake-pipeline)]
            (vcr/with-cassette {:name :observation-pipeline-is-transactional-2
                                :recordable? setup/not-localhost?}
              (etl/observation-pipeline system)))
          (catch Exception _ nil))
        ;; When we try again, we should load all the observations (no left over
        ;; state from the failed run)
        (is (= total-observations
               (vcr/with-cassette {:name :observation-pipeline-is-transactional-3
                                   :recordable? setup/not-localhost?}
                 (etl/observation-pipeline system)))))))
