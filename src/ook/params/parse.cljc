(ns ook.params.parse
  (:require
   [ook.util :as u]
   [clojure.string :as str]))

(defn get-query [{:keys [query-params]}]
  (get query-params "q"))

(defn parse-filters [{:keys [query-params]}]
  (some->> (get query-params "code")
           u/box
           (map #(str/split % #","))
           (map (fn [[id scheme]] {:id id :scheme scheme}))))
