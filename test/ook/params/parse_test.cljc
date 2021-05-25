(ns ook.params.parse-test
  (:require [clojure.test :refer [deftest is testing]]
            [ook.params.parse :as sut]))

(let [filter-state {"Product"
                    {"data/gss_data/trade/ons-pink-book-trade-in-services#scheme/pink-book-services"
                     #{"data/gss_data/trade/ons-pink-book-trade-in-services#concept/pink-book-services/10.3.2"}}}]
  #?(:cljs
     (deftest cljs-round-tripping-filter-state-test
       (testing "works with empty filter state"
         (let [encoded (sut/serialize-filter-state {})]
           (is (= "y2Dy1JNvyeAhh2R1f6EYn8XotFEx7MEEhcT" encoded))
           (is (= {} (sut/deserialize-filter-state encoded)))))

       (testing "works with nil filter state"
         (is (= nil (sut/serialize-filter-state nil)))
         (is (= nil (sut/deserialize-filter-state nil))))

       (testing "gzips and base58 encodes and decodes filter state"
         (let [encoded (sut/serialize-filter-state filter-state)]

           (is (= "AcNGXCgrRzto9cvt16NhTiUe9vvaykiUHSRBkcgpXMkBGg7QDLDZz6tG6LoqYYnMjcCkrcLTH3DzNDuD6DZMisLjrTunq9VHDzYaEo4LLWgYeSX93G9q4gcHBGww9TsHAE4wJDxqZhNdT3VybygbWs6JLCHUag3cGvhHuvRa8j8T" encoded))
           (is (= filter-state (sut/deserialize-filter-state encoded))))))

     :clj
     (deftest clj-deserializing-filter-state
       (testing "works with empty filter state"
         (is (= {} (sut/deserialize-filter-state "y2Dy1JNvyeAhh2R1f6EYn8XotFEx7MEEhcT"))))

       (testing "works with nil filter state"
         (is (= nil (sut/deserialize-filter-state nil))))

       (testing "can understand an encoded filter state"
         (is (= filter-state
                (sut/deserialize-filter-state "AcNGXCgrRzto9cvt16NhTiUe9vvaykiUHSRBkcgpXMkBGg7QDLDZz6tG6LoqYYnMjcCkrcLTH3DzNDuD6DZMisLjrTunq9VHDzYaEo4LLWgYeSX93G9q4gcHBGww9TsHAE4wJDxqZhNdT3VybygbWs6JLCHUag3cGvhHuvRa8j8T")))))))
