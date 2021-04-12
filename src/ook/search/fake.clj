(ns ook.search.fake
  (:require [integrant.core :as ig]
            [ook.search.db :as db]
            [clojure.string :as str]))

(defrecord FakeSearch []
  db/SearchBackend

  (get-datasets-for-facets [_ facets]
    (condp = facets
      {"facet1" ["codelist1"]} "valid response 1"

      {"facet1" ["codelist1" "codelist2"]} "valid response 2"

      {"facet1" ["codelist1"]
       "facet2" ["codelist2"]} "valid response 3"))

  (get-facets [_]
    [{:name "facet"}])

  (all-datasets [_]
    ["all datasets..."])

  (dataset-count [_]
    20)

  (components->codelists [_ uris]
    (if (seq uris)
      (str "codelists for " (str/join ", " uris))
      []))

  (get-concept-tree [_ codelist]
    (if codelist
      (str "concept tree for " codelist)
      [])))

(defmethod ig/init-key :ook.search.fake/db [_ _]
  (->FakeSearch))
