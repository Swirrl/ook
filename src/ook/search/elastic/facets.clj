(ns ook.search.elastic.facets
  (:require [ook.search.elastic.util :as esu]
            [clojurewerkz.elastisch.rest.document :as esd]))

(defn- find-child-dimensions [conn parent-dimension]
  (->> (esd/search conn "component" "_doc"
                   {:query {:term {:subPropertyOf parent-dimension}}})
       :hits :hits
       (map :_id)
       (into [])))

(defn- replace-parent-dimension-with-children [conn {:keys [parent-dimension] :as facet}]
  (if parent-dimension
    (let [dimensions (find-child-dimensions conn parent-dimension)]
      (-> facet (dissoc :parent-dimension) (assoc :dimensions dimensions)))
    facet))

(defn get-facets
  "Resolves a facet configuration by looking up sub-properties"
  [{:keys [elastic/endpoint ook/facets]}]
  (let [conn (esu/get-connection endpoint)]
    (map (partial replace-parent-dimension-with-children conn) facets)))
