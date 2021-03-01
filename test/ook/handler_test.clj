(ns ook.handler-test
  (:require [clojure.test :refer [deftest testing is]]
            [ook.test.util.setup :as setup]
            [ring.mock.request :as req]
            [ook.test.util.misc :as misc]
            [clojure.string :as str]))

;; (deftest handler-test
;;   (setup/with-test-system
;;     system

;;     (let [handler (:ook.concerns.reitit/ring-handler system)
;;           make-request (fn [path content-type] (-> (req/request :get (misc/with-test-host path))
;;                                                    (assoc-in [:headers "accept"] content-type)
;;                                                    (req/content-type content-type)
;;                                                    handler))
;;           request-html (fn [path] (make-request path "text/html"))]

;;       ;; (testing "home handler works"
;;       ;;   (let [response-body (:body (request-html ""))]
;;       ;;     (is (str/includes? response-body "Search"))
;;       ;;     (is (not (str/includes? response-body "Found")))))

;;       ;; (testing "search handler works with no query param"
;;       ;;   (let [response-body (:body (request-html "search"))]
;;       ;;     (is (str/includes? response-body "Found 0 codes"))))

;;       ;; (testing "search handler works with a query param"
;;       ;;   (let [response-body (:body (request-html "search?q=test"))]
;;       ;;     (is (str/includes? response-body "http://test"))
;;       ;;     (is (str/includes? response-body "This is a test label"))))

;;       ;; (testing "search handler returns transit when requested"
;;       ;;   (let [response (make-request "search?q=test" "application/transit+json")]
;;       ;;     (is (= "application/transit+json" (-> response :headers (get "Content-Type"))))
;;       ;;     (is (str/includes? (:body response) "\"~:label\",\"This is a test label\""))))
;;       )))
