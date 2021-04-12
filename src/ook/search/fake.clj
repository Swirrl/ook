(ns ook.search.fake
  (:require [integrant.core :as ig]
            [ook.search.db :as db]))

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

  (components->codelists [_ uris]
    ["codelist-uri"])

  (all-datasets [_]
    ["all datasets..."])

  (dataset-count [_]
    20))

(defmethod ig/init-key :ook.search.fake/db [_ _]
  (->FakeSearch))
