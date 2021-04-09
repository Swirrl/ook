(ns ook.search.elastic.facets
  (:require [ook.search.elastic.util :as esu]
            [ook.util :as util]
            [clojurewerkz.elastisch.rest.document :as esd]))

(defn- find-child-dimensions [conn parent-dimension]
  (->> (esd/search conn "component" "_doc"
                   {:query {:term {:subPropertyOf parent-dimension}}})
       :hits :hits
       (map :_id)
       (into [])))

(defn- replace-parent-dimension-with-dimensions [conn {:keys [parent-dimension] :as facet}]
  (if parent-dimension
    (let [child-dimensions (find-child-dimensions conn parent-dimension)
          dimensions (cons parent-dimension child-dimensions)]
      (-> facet
          (dissoc :parent-dimension)
          (assoc :dimensions dimensions)))
    facet))

(defn get-facets
  "Resolves a facet configuration by looking up sub-properties"
  [{:keys [ook/facets elastic/endpoint]}]
  (let [conn (esu/get-connection endpoint)]
    (map (partial replace-parent-dimension-with-dimensions conn) facets)))

(defn get-facets-for-selections
  "Gets only those facets that are included in the selections"
  [selections opts]
  (filter (fn [f] (contains? selections (:name f))) (get-facets opts)))

(defn- join-code-selections
  "Combines two (possibly empty) vectors of code URIs (i.e. for a common dimension).
  If either is empty (i.e. any code could match) then the overall result is empty (the wildcard makes
  the specifications redundant). If both are non-empty then the set union is returned."
  [codes-a codes-b]
  (if (or (empty? codes-a)
          (empty? codes-b))
    []
    (distinct (concat codes-a codes-b))))

(defn dimension-selections
  "Given a map from facet name to a map from codelist to a (possibly empty) vector of codes,
  return a map from dimension to a (possibly empty) vector of codes (merging across facets)"
  [codelist-selections dimensions-lookup]
  (->> (for [[facet selection] codelist-selections]
         (for [[codelist codes] selection]
           (for [dimension (dimensions-lookup codelist)]
             {dimension codes})))
       flatten
       (apply merge-with join-code-selections)))
