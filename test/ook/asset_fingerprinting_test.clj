(ns ook.asset-fingerprinting-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [ook.concerns.asset-fingerprinting :as sut]))

(deftest get-fingerprinted-path-test
  (testing "for new paths"
    (testing "returns the fingerprinted path"
      (is (= {} @sut/asset-db))
      (is (= "/assets/css/test1-1aa88991.css"
             (sut/get-fingerprinted-path "ook/resources" "/assets/css/test1.css"))))

    (testing "saves the fingerprinted path"
      (is (= "ook/resources/css/test1.css"
             (get @sut/asset-db "/assets/css/test1-1aa88991.css")))))

  (testing "for saved paths"
    (testing "returns the fingerprinted path if it's requested"
      (is (= {"/assets/css/test1-1aa88991.css" "ook/resources/css/test1.css"}
             @sut/asset-db))
      (is (= "/assets/css/test1-1aa88991.css"
             (sut/get-fingerprinted-path "ook/resources" "/assets/css/test1.css")))))

  (testing "for a file that doesn't exist"
    (testing "returns nil"
      (is (nil? (sut/get-fingerprinted-path "ook/resources" "/does/not/exist.js")))))

  (reset! sut/asset-db {}))

(deftest resource-response)
