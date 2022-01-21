(ns ook.search.elastic.text
  (:require [clojure.string :as st]
            [clojurewerkz.elastisch.rest.document :as esd]
            [ook.search.elastic.util :as esu]
            [ook.search.elastic.components :as components]
            [ook.util :as u]))

(def size-limit 1000)

(defn- append-id [x] (str x ".@id"))

(defn- terms-clauses
  "Vector of terms clauses to find one of more exact value for each dimension"
  [selection]
  (map (fn [[d vs]] {:terms {(append-id d) vs}})
       selection))

(defn- observation-query
  "Creates a query for finding observations with selected dimensions and dimension-values.

  `selection` is a map from dimension-uri to a vector of dimension-value-uris (all strings).
  e.g. `{ \"data/mydim\" [\"concept/a\" \"concept/b\"] }`

  All criteria are combined with 'should' i.e. OR. Provides counts of observations by dataset
  and identifies the top three codes by dimension"
  [selection]
  {:size 0
   :query {:bool {:should (terms-clauses selection)}} ;;:minimum_should_match "2"
   :aggregations {:cubes {:terms {:field "qb:dataSet.@id" :size size-limit}}}})

(defn observation-hits [selection {:keys [elastic/conn]}]
  (esd/search conn "observation" "_doc" (observation-query selection)))

(defn codes
  "Queries for codes"
  [query {:keys [elastic/conn]}]
  (->> (esd/search conn "code" "_doc"
                   {:query {:bool {:must [{:match {:label {:query query}}}
                                          {:term {:used "true"}}]}}
                    :size size-limit})
       :hits :hits
       (map :_source)
       (map esu/normalize-keys)))

(defn codes-to-selection
  "Creates a `selection` map (from dimension to values) from an sequence of codes"
  [codes {:keys [elastic/conn]}]
  (let [codes-for-codelist (->> codes
                                (mapcat (fn [{:keys [ook/uri scheme]}]
                                          (let [schemes (u/box scheme)]
                                            (map (fn [scheme] {scheme [uri]}) schemes))))
                                (apply merge-with concat))
        codelist-uris (distinct (mapcat (comp u/box :scheme) codes))
        components (->> (esd/search conn "component" "_doc"
                                    {:query
                                     {:nested
                                      {:path "codelist"
                                       :query {:terms {"codelist.@id" codelist-uris}}}}
                                     :size size-limit})
                        :hits :hits
                        (map :_source)
                        (map esu/normalize-keys))]

    (apply merge (map (fn [{:keys [ook/uri codelist]}]
                        { uri (codes-for-codelist (:ook/uri codelist))})
                      components))))

(defn- match-describer
  "Returns a function to add description to a dataset explaining how it matches"
  [cubes codes]
  (fn [dataset]
    (let [cube (first (filter #(= (:cube dataset) (:key %)) cubes))
          code-lookup (u/lookup-coll :scheme codes)]
      (-> dataset
          (assoc
           :matching-observation-count
           (:doc_count cube))
          (update
           :component
           (fn [components]
             (map (fn [component]
                    (let [matches (code-lookup (get-in component [:codelist :ook/uri]))]
                      (if (not-empty matches)
                        (assoc component :matches matches)
                        component)))
                  components)))))))

(defn datasets-with-components
  "Retrieves dataset definitions (with components defined) for cube-uris"
  [cube-uris {:keys [elastic/conn]}]
  (let [datasets (->> (esd/search conn "dataset" "_doc"
                                  {:query {:terms {"cube" cube-uris}}
                                   :size size-limit})
                      :hits :hits
                      (map :_source)
                      (map esu/normalize-keys))
        component-lookup (->> (esd/search conn "component" "_doc"
                                          {:query {:terms {"@id" (mapcat :component datasets)}}
                                           :size size-limit})
                              :hits :hits
                              (map :_source)
                              (map esu/normalize-keys)
                              (map #(select-keys % [:ook/uri :label :codelist]))
                              u/id-lookup)]
    (map (fn [d] (update d :component #(map (fn [c] (component-lookup c))
                                            %))) datasets)))

(defn datasets-from-results
  "Converts observation hits into a seq of dataset results for display"
  [hits codes {:keys [elastic/conn] :as opts}]
  (let [cubes (get-in hits [:aggregations :cubes :buckets])
        cube-uris (map (fn [d] {:ook/uri (:key d)}) cubes)
        datasets (datasets-with-components cube-uris opts)
        describe-matches (match-describer cubes codes)]
    (map describe-matches datasets)))

(defn ordered [datasets]
  (let [rank (fn [dataset]
               (count (filter #(not-empty (:matches %))
                              (:component dataset))))]
    (sort-by rank > datasets)))

(defn dataset-search
  "Find datasets by search code labels"
  [query opts]
  (let [codes (codes query opts)]
    (if (seq codes)
      (let [selection (codes-to-selection codes opts)
            hits (observation-hits selection opts)
            datasets (datasets-from-results hits codes opts)]
        (ordered datasets))
      (list))))

(comment
  (let [opts {:elastic/conn
              (esr/connect "http://localhost:9200" {:content-type :json})}]
    (dataset-search "imports of cars from Germany" opts)))
