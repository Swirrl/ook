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
      (testing "Can create indicies"
        (is (every? acknowledged? (vals responses))))
      (testing "Indicies have mappings"
        (let [mapping (sut/get-mapping system "code")]
          (is (= (get-in mapping [:code :mappings :properties :scheme :type])
                 "keyword")
              (str "Code mapping is " mapping)))))
    (let [responses (sut/delete-indicies system)]
      (is (every? acknowledged? (vals responses))))))
