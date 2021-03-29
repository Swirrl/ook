(ns ook.search.elastic.util
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojurewerkz.elastisch.rest :as esr]))

(defn get-connection [endpoint]
  (esr/connect endpoint {:content-type :json}))

(defn normalize-keys [m]
  (let [remove-at (fn [[k v]]
                    (if (str/includes? (str k) "@")
                      [(keyword (str/replace k ":@" "")) v]
                      [k v]))]
    ;; only apply to maps
    (walk/postwalk (fn [x] (if (map? x) (into {} (map remove-at x)) x))
                   m)))
