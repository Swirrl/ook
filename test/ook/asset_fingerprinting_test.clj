(ns ook.asset-fingerprinting-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [clojure.set :as set]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [ring.mock.request :as req]
   [ook.concerns.asset-fingerprinting :as sut]))

(deftest get-fingerprinted-path-test
  (testing "for new paths"
    (testing "returns the fingerprinted path"
      (is (= {} @sut/asset-db))
      (is (= "/route/css/test1-1aa88991.css"
             (sut/get-fingerprinted-path "ook/resources" "/route/css/test1.css"))))

    (testing "saves the fingerprinted path"
      (is (= "ook/resources/css/test1.css"
             (get @sut/asset-db "/route/css/test1-1aa88991.css")))))

  (testing "for saved paths"
    (testing "returns the fingerprinted path if it's requested"
      (is (= {"/route/css/test1-1aa88991.css" "ook/resources/css/test1.css"}
             @sut/asset-db))
      (is (= "/route/css/test1-1aa88991.css"
             (sut/get-fingerprinted-path "ook/resources" "/route/css/test1.css")))))

  (testing "for a file that doesn't exist"
    (testing "returns nil"
      (is (nil? (sut/get-fingerprinted-path "ook/resources" "/does/not/exist.js")))))

  (reset! sut/asset-db {}))

(defn resource-request [path]
  (-> (req/request :get path)
      (assoc :uri path)))

(def opts {:assets/root "ook/resources"})

(deftest resource-response-test
  (testing "when requested path matches a hashed file"
    (sut/get-fingerprinted-path "ook/resources" "/route/css/test1.css")
    (sut/get-fingerprinted-path "ook/resources" "/route/css/to-change.css")

    (testing "returns the actual file"
      (let [response (sut/resource-handler
                      opts
                      (resource-request "/route/css/test1-1aa88991.css"))]
        (is (= java.io.File (type (:body response))))
        (is (str/includes? (str (:body response)) "ook/resources/css/test1.css"))))

    (testing "does not change the stored checksum if the contents are the same"
      (is (= "/route/css/to-change-9e87afc4.css"
             (-> @sut/asset-db set/map-invert (get "ook/resources/css/to-change.css"))))

      (sut/resource-handler
       opts
       (resource-request "/route/css/to-change-9e87afc4.css"))

      (is (= "/route/css/to-change-9e87afc4.css"
             (-> @sut/asset-db set/map-invert (get "ook/resources/css/to-change.css")))))

    (testing "when the file contents have changed"
      (is (= "/route/css/to-change-9e87afc4.css"
             (-> @sut/asset-db set/map-invert (get "ook/resources/css/to-change.css"))))

      (let [original-content (slurp (io/resource "ook/resources/css/to-change.css"))]
        (spit (io/resource "ook/resources/css/to-change.css") "/* different-content */")

        (testing "updates the checksum in the asset db"
          (sut/resource-handler
           opts
           (resource-request "/route/css/to-change-9e87afc4.css"))

          (is (= "/route/css/to-change-1e7137cc.css"
                 (-> @sut/asset-db set/map-invert (get "ook/resources/css/to-change.css")))))

        (testing "returns the right file on subsequent requests with this updated content"
          (let [response (sut/resource-handler
                          opts
                          (resource-request "/route/css/to-change-1e7137cc.css"))]

            (is (str/includes? (str (:body response)) "ook/resources/css/to-change.css"))))

        (spit (io/resource "ook/resources/css/to-change.css") original-content)))

    (testing "includes the right mimetype"
      (is (= "text/css"
             (-> (sut/resource-handler
                  opts
                  (resource-request "/route/css/test1-1aa88991.css"))
                 (get-in [:headers "Content-Type"]))))

      (is (= "text/javascript"
             (-> (sut/resource-handler
                  opts
                  (resource-request "/route/css/test1.js"))
                 (get-in [:headers "Content-Type"])))))

    (testing "includes the checksum in an etag header"
      (is (= "1aa88991"
             (-> (sut/resource-handler
                  opts
                  (resource-request "/route/css/test1-1aa88991.css"))
                 (get-in [:headers "ETag"]))))))

  (testing "when the requested path does not match a hashed file"
    (testing "passes on the request if the file does exist"
      (is (= "test2.css"
             (.getName (:body (sut/resource-handler
                               opts
                               (resource-request "/route/css/test2.css")))))))

    (testing "returns 404 response if requested path does not match any file"
      (let [response (sut/resource-handler
                      opts
                      (resource-request "/route/not-found.css"))]
        (is (= 404 (:status response))))))

  (testing "returns 404 if the request is not a read request"
    (let [response (sut/resource-handler
                    opts
                    (req/request :post "resource/path.js"))]
      (is (= 404 (:status response)))))

  (reset! sut/asset-db {}))
