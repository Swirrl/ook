(ns ook.index-test
  (:require [ook.index :as sut]
            [clojure.test :refer :all]
            [ook.test.util.setup :refer [with-system]]
            [clojurewerkz.elastisch.rest :as cer]
            [ook.search.elastic.util :as esu]))

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
      (testing "Analyser settings"
        (let [conn (-> system :ook.concerns.elastic/endpoint esu/get-connection)
              tokens (fn [input]
                       ;; clojurewerkz.elastisch.rest.document/analyze doesn't work here
                       ;; seems like ES is expecting a json request body on a get request!
                       (->> (cer/post conn (cer/analyze-url conn "code")
                                      {:body {:text input :analyzer :ook_std}})
                            :tokens
                            (map :token)))]
          (testing "Stop words are filtered"
            (is (not-any? #{"but" "for" "such" "as" "there"}
                          (tokens "but for many such words as there are"))))
          (testing "Stemming is applied"
            (is (= (tokens "imports of cars")
                   ["import" "car"]))))))
    (let [responses (sut/delete-indicies system)]
      (is (every? acknowledged? (vals responses))))))
