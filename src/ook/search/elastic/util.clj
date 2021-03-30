(ns ook.search.elastic.util
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojurewerkz.elastisch.rest :as esr]))

(defn get-connection [endpoint]
  (esr/connect endpoint {:content-type :json}))

(defn- replace-problematic-char [k]
  (if (= (keyword "@id") k)
    :ook/uri
    (keyword (str/replace k ":@" "ook/"))))

(defn normalize-keys [m]
  (let [remove-at (fn [[k v]]
                    (if (str/includes? (str k) "@")
                      [(replace-problematic-char k) v]
                      [k v]))]
    ;; only apply to maps
    (walk/postwalk (fn [x] (if (map? x) (into {} (map remove-at x)) x))
                   m)))
