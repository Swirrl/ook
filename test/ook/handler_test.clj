(ns ook.handler-test
  (:require [clojure.test :refer [deftest testing is]]
            [ook.test.util.setup :as setup]
            [ring.mock.request :as req]
            [ook.test.util.misc :as misc]
            [ook.concerns.integrant :as i]
            [clojure.string :as str]))

(deftest handler-test
  (i/with-system [system setup/test-profiles]
    system

    (let [handler (:ook.concerns.reitit/ring-handler system)
          make-request (fn [path content-type] (-> (req/request :get (misc/with-test-host path))
                                                   (assoc-in [:headers "accept"] content-type)
                                                   (req/content-type content-type)
                                                   handler))
          request-html (fn [path] (make-request path "text/html"))
          request-transit (fn [path] (make-request path "application/transit+json"))]

      (testing "html routes"
        (testing "/ returns html" ;; note this won't have js, really a smoke test
          (let [response-body (:body (request-html ""))]
            (is (str/includes? response-body "Structural Search"))
            (is (not (str/includes? response-body "Found")))))

        (testing "/search returns html"
          (let [response-body (:body (request-html "search"))]
            (is (str/includes? response-body "Structural Search"))))

        (testing "/not-a-route returns 404"
          (let [response (request-html "not-a-route")]
            (is (= 404 (:status response)))
            (is (= "404" (:body response))))))

      (testing "internal api routes"
        (testing "/get-codes rejects html requests"
          (let [response (request-html "get-codes?q=test")]
            (is (= 406 (:status response)))
            (is (= "Unsupported content type" (:body response)))))

        (testing "/get-codes returns data when transit is requested"
          (let [response (request-transit "get-codes?q=test")]
            (is (= 200 (:status response)))
            (is (str/includes? (:body response) "\"~:label\",\"This is a test label\""))))

        (testing "/get-codes can handle empty query"
          (let [response (request-transit "get-codes?q=")]
            (is (= 200 (:status response)))
            (is (= "[]" (:body response)))))

        (testing "/apply-filters rejects html requests"
          (let [response (request-html "apply-filters?code=a-code,scheme-1")]
            (is (= 406 (:status response)))
            (is (= "Unsupported content type" (:body response)))))

        (testing "/apply-filters can parse no param"
          (let [response (request-transit "apply-filters?code=")]
            (is (= 200 (:status response)))
            (is (= (:body response) "[]"))))

        (testing "/apply-filters can parse a single code param"
          (let [response (request-transit "apply-filters?code=a-code,scheme-1")]
            (is (= 200 (:status response)))
            (is (str/includes? (:body response) "valid response 1"))))

        (testing "/apply-filters can parse multiple code params"
          (let [response (request-transit "apply-filters?code=a-code,scheme-1&code=another-code,scheme-2")]
            (is (= 200 (:status response)))
            (is (str/includes? (:body response) "valid response 2"))))

        (testing "/datasets rejects html requests"
          (let [response (request-html "datasets")]
            (is (= 406 (:status response)))
            (is (= "Unsupported content type" (:body response)))))

        (testing "/datasets works"
          (let [response (request-transit "datasets")]
            (is (= 200 (:status response)))
            (is (str/includes? (:body response) "datasets"))))))))
