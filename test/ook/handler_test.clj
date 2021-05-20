(ns ook.handler-test
  (:require [clojure.test :refer [deftest testing is]]
            [ook.test.util.setup :as setup :refer [with-system]]
            [ring.mock.request :as req]
            [ook.test.util.misc :as misc]
            [clojure.string :as str]))

(deftest handler-test
  (with-system [system setup/test-profiles]
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
            (is (str/includes? response-body "Find trade data"))
            (is (not (str/includes? response-body "Found")))))

        (testing "/search returns html"
          (let [response-body (:body (request-html "search"))]
            (is (str/includes? response-body "Find trade data"))))

        (testing "/not-a-route returns 404"
          (let [response (request-html "not-a-route")]
            (is (= 404 (:status response)))
            (is (str/includes? (:body response) "not found")))))

      (testing "internal api routes"
        (testing "/datasets"

          (testing "rejects html requests"
            (let [response (request-html "datasets")]
              (is (= 406 (:status response)))
              (is (= "Unsupported content type" (:body response)))))

          (testing "with no facet params returns all (unfiltered) datasets"
            (let [response (request-transit "datasets")]
              (is (= 200 (:status response)))
              (is (= "application/transit+json" (get-in response [:headers "Content-Type"])))
              (is (str/includes? (:body response) "all datasets..."))))

          (testing "with filters works"
            (let [response (request-transit
                            "datasets?filters=%5B%22%5E%20%22%2C%22facet1%22%2C%5B%22%5E%20%22%2C%22codelist1%22%2C%5B%22code1%22%5D%5D%5D")]
              (is (= 200 (:status response)))
              (is (= "application/transit+json" (get-in response [:headers "Content-Type"])))
              (is (str/includes? (:body response) "valid response"))))))

      (testing "/codes"
        (testing "rejects html requests"
          (let [response (request-html "codes")]
            (is (= 406 (:status response)))
            (is (= "Unsupported content type" (:body response)))))

        (testing "works with no query param"
          (let [response (request-transit "codes")]
            (is (= 200 (:status response)))
            (is (= "application/transit+json" (get-in response [:headers "Content-Type"])))
            (is (= "[]" (:body response)))))

        (testing "calls the db with a codelist when there is one"
          (let [response (request-transit "codes?codelist=cl1")]
            (is (= 200 (:status response)))
            (is (= "application/transit+json" (get-in response [:headers "Content-Type"])))
            (is (str/includes? (:body response) "concept tree for cl1")))))

      (testing "/codelists"
        (testing "rejects html requests"
          (let [response (request-html "codelists")]
            (is (= 406 (:status response)))
            (is (= "Unsupported content type" (:body response)))))

        (testing "doesn't blow up with no query param"
          (let [response (request-transit "codelists")]
            (is (= 200 (:status response)))
            (is (= "application/transit+json" (get-in response [:headers "Content-Type"])))
            (is (= "[]" (:body response)))))

        (testing "calls the db with dimensions when they're there"
          (let [response (request-transit "codelists?dimension=dim1")]
            (is (= 200 (:status response)))
            (is (= "application/transit+json" (get-in response [:headers "Content-Type"])))
            (is (str/includes? (:body response) "codelists for dim1"))))))))
