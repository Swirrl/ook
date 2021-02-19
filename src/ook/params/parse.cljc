(ns ook.params.parse
  (:require [clojure.string :as str]))

(defn get-query [{:keys [query-params]}]
  (get query-params "q"))

(defn- box [v]
  (if (coll? v) v [v]))

(defn parse-filters [{:keys [query-params]}]
  (->> (get query-params "code")
       box
       (map #(str/split % #","))
       (map (fn [[id scheme]] {:id id :scheme scheme}))))
