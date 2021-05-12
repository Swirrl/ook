(ns ook.concerns.asset-fingerprinting
  (:require
   [integrant.core :as ig]
   [ring.util.response :as response]
   [clojure.set :as set]
   [clojure.tools.logging :as log]
   [clojure.string :as str]
   [clojure.java.io :as io])
  (:import (java.util.zip CRC32)))

(def asset-db
  "DB of generated fingerprinted paths and their actual resource location
  on the JVM classpath, e.g.

  {assets/css/styles-123123.css public/css/styles.css
   assets/js/main-123123.css public/js/main.js}"
  (atom {}))

;; crc32 checksums are fast to generate https://github.com/xsc/pandect#benchmark-results
(defn- get-checksum [resource]
  (let [contents (slurp resource)
        crc (new CRC32)]
    (. crc update (. contents getBytes))
    (Long/toHexString (. crc getValue))))

(defn- splice-into-file-name [checksum path]
  (str/replace
    path
    (re-pattern
      (str "(.+/)" ;; everything up to the last slash
           "([^/.]+)" ;; everything between the last slash and the dot, i.e. the file name
           "(..+$)" ;; everything between the last dot and the end, i.e. the extension
           ))
    (str "$1$2-" checksum "$3")))

(defn- fingerprint-path [requested-path resource-path]
  (when-let [resource (io/resource resource-path)]
    (let [checksum (get-checksum resource)]
      (splice-into-file-name checksum requested-path))))

(defn- strip-leading-slash [path]
  (str/replace path #"^\/" ""))

(defn- drop-first-path-segment
  "Drops the first segment of a path because this is the part that is used
   for matching by the router, not relevant to finding the actual file."
  [path]
  (str/replace path #"^[^\/.]+\/" ""))

(defn- get-resource-path [root requested-path]
  (->> requested-path
       strip-leading-slash
       drop-first-path-segment
       (str root "/")))

(defn- fingerprint-and-save-path! [root requested-path]
  (let [resource-path (get-resource-path root requested-path)]
    (if-let [fingerprinted-path (fingerprint-path requested-path resource-path)]
      (do
        (swap! asset-db assoc fingerprinted-path resource-path)
        fingerprinted-path)
      (log/error (str "Could not create checksum for resource "
                      resource-path ". "
                      "Does it exist? Is the resource on the classpath?")))))

(defn get-fingerprinted-path
  "Create a fingerprinted asset uri in the asset-db and return
  that uri. Function with side-effects, but idempotent."
  [root requested-resource-path]
  (if-let [fingerprinted-path (-> @asset-db
                                  set/map-invert
                                  (get (get-resource-path root requested-resource-path)))]
    fingerprinted-path
    (fingerprint-and-save-path! root requested-resource-path)))

(defmethod ig/init-key :ook.concerns.asset-fingerprinting/fingerprinter [_ {:keys [assets/root]}]
  (fn [path]
    (get-fingerprinted-path root path)))

;; (defn asset-resource-request
;;   "If request uri translates to a resource uri in the classpath and the resource
;;   file is found, returns it in a response map. Otherwise returns nil."
;;   [request]
;;   (when (mw/read-request? request)
;;     (when-let [[requested-asset-url url-fingerprint] (re-matches assets-route-pattern (req/path-info request))]
;;       (when-let [resource-path (original-resource-path requested-asset-url)]
;;         (let [latest-fingerprint (resource->crc32 (io/resource resource-path))]
;;           (when (not= latest-fingerprint url-fingerprint)
;;             (swap! asset-db dissoc requested-asset-url))
;;           (-> (resp/resource-response resource-path)
;;               (head/head-response request)
;;               (resp/header "ETag" latest-fingerprint)))))))

(defn- requested-path->resource-path [path]
  ;; check if it matches any hashed file
  ;; if not, re-hash and store it
  ;; return the actual file path
  (str "public/" path)
  ;; (get @asset-db path)
  )

(defn resource-response
  "Convert the fingerprinted path to the actual one then make a ring response, which
   returns nil if the file does not exist. It should, because we check when the
   fingerprinted names are generated, but in the edge case that the file goes missing
   in between, return a 404."
  [request]
  (let [;; reitit uses a blank keyword for the path params by default
        requested-path (-> request :path-params (get (keyword "")))
        path (requested-path->resource-path requested-path)
        response (response/resource-response path)]
    (if (and path response)
  ;; head request
  ;; add etag, head condition, mimetype
      response ;; add e-tag, mimetype
      {:status 404, :body "", :headers {}})))
