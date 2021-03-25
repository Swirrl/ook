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

(defn- replace-parent-dimension-with-children [conn {:keys [parent-dimension] :as facet}]
  (if parent-dimension
    (let [dimensions (find-child-dimensions conn parent-dimension)]
      (-> facet (dissoc :parent-dimension) (assoc :dimensions dimensions)))
    facet))

(defn get-facets
  "Resolves a facet configuration by looking up sub-properties"
  [{:keys [ook/facets elastic/endpoint]}]
  (let [conn (esu/get-connection endpoint)]
    (map (partial replace-parent-dimension-with-children conn) facets)))

(defn apply-selections
  "Applies components (replacing ids with docs) and selections (by filtering) to configured facets"
  [facets components selections]
  (let [->component (util/id-lookup components)
        matches-codelists? (fn [codelists]
                             (fn [c]
                               ((set codelists)
                                (:codelist c))))]
    (map (fn [facet]
           (let [selected-codelists (selections (:name facet))]
             (update facet :dimensions
                     (fn [ds] (->>
                               ds
                               (map ->component)
                               (filter (matches-codelists? selected-codelists)))))))
         facets)))

(defn selections-for-dataset [facet-selections dataset]
  (let [filter-to-dataset (partial filter (fn [d] ((set (:component dataset)) (util/id d))))]
    (->> facet-selections
         (map (fn [facet] (update facet :dimensions filter-to-dataset)))
         (filter (fn [facet] (not-empty (:dimensions facet) ))))))

(defn apply-facets
  "Returns a list of datasets that match the facet selections"
  [datasets components facets selections]
  (let [facet-selections (apply-selections facets components selections)]
    (->> datasets
         (map (fn [dataset] (assoc dataset :facets (selections-for-dataset facet-selections dataset))))
         (filter (fn [dataset] (not-empty (:facets dataset)))))))
