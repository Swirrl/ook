(ns ook.params.parse
  (:require
   [ook.util :as u]
   [clojure.string :as str]))

(defn get-query [{:keys [query-params]}]
  (get query-params "q"))

(defn parse-filters [{:keys [query-params]}]
  (let [code-param (get query-params "code")]
    (when (seq code-param)
      (some->> code-param
               u/box
               (map #(str/split % #","))
               (map (fn [[scheme id]] {:id id :scheme scheme}))))))
