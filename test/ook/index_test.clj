(ns ook.index-test
  (:require [ook.index :as sut]
            [clojure.test :refer :all]
            [ook.concerns.integrant :refer [with-system]]
            [integrant.core :as ig]
            [drafter-client.client.impl :as i]))

(defn acknowledged? [response]
  (true? (:acknowledged response)))

(deftest create-and-delete-index-test
  (with-system [system ["elasticsearch-test.edn"]]
    (let [responses (sut/create-indicies system)]
      (is (every? acknowledged? (vals responses))))
    (let [responses (sut/delete-indicies system)]
      (is (every? acknowledged? (vals responses))))))
