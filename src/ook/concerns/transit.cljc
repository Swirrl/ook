(ns ook.concerns.transit
  (:require
   ;; NOTE This require is sneaky -- it loads either transit-clj or transit-cljs,
   ;; depending on the environment, which have methods called the same thing but
   ;; with different signatures
   [cognitect.transit :as transit])
  #?(:clj (:import (java.io ByteArrayOutputStream))))

#?(:cljs (defn read-string [encoded-string]
           (transit/read (transit/reader :json) encoded-string)))

#?(:clj (defn write-string [unencoded-string]
          (let [stream (ByteArrayOutputStream.)
                writer (transit/writer stream :json)]
            (transit/write writer unencoded-string)
            (str stream))))
