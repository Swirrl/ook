(ns ook.params.parse
  (:require
   [ook.util :as u]
   [clojure.string :as str]))

(defn get-query [{:keys [query-params]}]
  (get query-params "q"))

(defn parse-filters [{:keys [query-params]}]
  (let [param (get query-params "facet")]
    (when (seq param)
      (some->> param
               u/box
               (map #(str/split % #","))
               (map (fn [[dim val]] {:value val :dimension dim}))))))
