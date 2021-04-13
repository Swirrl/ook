(ns ook.concerns.transit
  (:refer-clojure :exclude [read-string])
  (:require
   ;; NOTE This require is sneaky -- it loads either transit-clj or transit-cljs,
   ;; depending on the environment, which have functions with the same name but
   ;; different signatures
   [cognitect.transit :as transit])
  #?(:clj (:import (java.io ByteArrayOutputStream ByteArrayInputStream))))

(defn read-string [encoded-string]
  #?(:clj
     (let [stream (ByteArrayInputStream. (.getBytes encoded-string))
           reader (transit/reader stream :json)]
       (transit/read reader))

     :cljs
     (transit/read (transit/reader :json) encoded-string)))

(defn write-string [unencoded-string]
  #?(:clj
     (let [stream (ByteArrayOutputStream.)
           writer (transit/writer stream :json)]
       (transit/write writer unencoded-string)
       (str stream))

     :cljs
     (let [writer (transit/writer :json)]
       (transit/write writer unencoded-string))))
