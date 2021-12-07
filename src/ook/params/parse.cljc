(ns ook.params.parse
  (:require
   #?@(:cljs [["pako" :as pako]])
   #?@(:clj [[clojure.java.io :as io]])
   [alphabase.base58 :as base58]
   [ook.util :as u]
   [ook.concerns.transit :as t])
  #?(:clj (:import [java.util.zip GZIPInputStream])))

#?(:cljs
   (defn serialize-filter-state [filter-state]
     (some-> filter-state t/write-string pako/gzip base58/encode)))

(defn gzipped-bytes->str [bytes]
  #?(:cljs
     (when bytes
       (pako/ungzip bytes #js{:to "string"}))

     :clj
     (when bytes
       (with-open [in (GZIPInputStream. (io/input-stream bytes))]
         (slurp in)))))

(defn deserialize-filter-state [encoded-filter-state]
  (some-> encoded-filter-state base58/decode gzipped-bytes->str t/read-string))

(defn get-facets
  [{:keys [query-params]}]
  (when (seq query-params)
    (-> query-params (get "filters") deserialize-filter-state)))

(defn get-dimensions [{:keys [query-params]}]
  (when (seq query-params)
    (-> query-params (get "dimension") u/box)))

(defn get-codelist [{:keys [query-params]}]
  (when (seq query-params)
    (get query-params "codelist")))

(defn get-search-params [{:keys [query-params]}]
  {:search-term (query-params "search-term")
   :codelists (u/box (query-params "codelists"))})

(defn query [{:keys [query-params]}]
  (query-params "query"))
