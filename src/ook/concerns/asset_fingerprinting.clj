(ns ook.concerns.asset-fingerprinting
  (:require
   [integrant.core :as ig]
   [ring.util.response :as response]
   [ring.util.mime-type :as mime-type]
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

(def extension-pattern
  "Everything from the last dot to the end"
  "(\\..+$)")

(def parent-dirs-pattern
  "Everything up to the last slash"
  "(^.+\\/)")

(defn- splice-into-file-name [checksum path]
  (str/replace
   path
   (re-pattern
    (str parent-dirs-pattern
         "([^/\\.]+)" ;; everything between the last slash and the dot, i.e. the file name
         extension-pattern
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

(defn- read-request? [request]
  (#{:get :head} (:request-method request)))

(def fingerprinted-path-pattern
  (re-pattern
   (str parent-dirs-pattern
        "(.+-)" ;; everything up to the last hyphen
        "(\\w{1,8})" ;; capture betweeen 1 and 8 word characeters
        extension-pattern
        )))

(defn- parse-fingerprint [requested-path]
  (let [[_ _ _ fingerprint _]
        (re-matches fingerprinted-path-pattern requested-path)]
    fingerprint))

(defn- replace-fingerprint [requested-path latest-fingerprint]
  (str/replace
   requested-path
   fingerprinted-path-pattern
   (str "$1$2" latest-fingerprint "$4")))

(defn- update-fingerprint!
  "Check the current fingerprint against the current contents of the file.
   If the contents have been updated, update the path in the asset db, and
   return the new fingerprint."
  [requested-path resource-path]
  (let [current-fingerprint (parse-fingerprint requested-path)
        latest-fingerprint (-> resource-path io/resource get-checksum)]
    (when-not (= current-fingerprint latest-fingerprint)
      (let [new-path (replace-fingerprint requested-path latest-fingerprint)]
        (swap! asset-db dissoc requested-path)
        (swap! asset-db assoc new-path resource-path)))
    latest-fingerprint))

(defn resource-handler
  "Convert the fingerprinted path to the actual one then make a ring response, which
   returns nil if the file does not exist. It should, because we check when the
   fingerprinted names are generated, but in the edge case that the file goes missing
   in between, return a 404."
  [{:keys [assets/root]} request]
  (let [requested-path (:uri request)
        resource-path (get @asset-db requested-path (get-resource-path root requested-path))
        response (response/resource-response resource-path)]
    (if (and (read-request? request) response)
      (let [latest-fingerprint (update-fingerprint! requested-path resource-path)]
        (-> response
            (response/content-type (mime-type/ext-mime-type resource-path))
            (response/header "ETag" latest-fingerprint)))
      {:status 404, :body "", :headers {}})))
