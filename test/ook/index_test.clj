(ns ook.index-test
  (:require [ook.index :as sut]
            [clojure.test :refer :all]
            [ook.test.util.setup :refer [with-system]]
            [clojurewerkz.elastisch.rest :as cer]))

(defn acknowledged? [response]
  (true? (:acknowledged response)))

(deftest create-and-delete-index-test
  (with-system [system ["elasticsearch-test.edn"]]
    (sut/delete-indicies system)
    (let [responses (sut/create-indicies system)]
      (testing "Can create indicies"
        (is (every? acknowledged? (vals responses))))
      (testing "Indicies have mappings"
        (let [mapping (sut/get-mapping system "code")]
          (is (= (get-in mapping [:code :mappings :properties :scheme :type])
                 "keyword")
              (str "Code mapping is " mapping))))
      (testing "Stop words analyser is configured"
        (let [conn (-> "http://localhost:9200" sut/connect)
              input "but for many such words as there are"
              ;; clojurewerkz.elastisch.rest.document/analyze doesn't work here
              ;; seems like ES is expecting a json request body on a get request!
              analysed (cer/post conn (cer/analyze-url conn "code")
                                 {:body {:text input
                                         :analyzer :std_english}})
              tokens (map :token (:tokens analysed))]
          (is (= tokens
                 ["many" "words"])))))
    (let [responses (sut/delete-indicies system)]
      (is (every? acknowledged? (vals responses))))))
