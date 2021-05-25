(ns ook.search.fake
  (:require [integrant.core :as ig]
            [ook.search.db :as db]
            [clojure.string :as str]))

(defrecord FakeSearch []
  db/SearchBackend

  (get-datasets-for-facets [_ filters]
    (if (= filters {"facet1" {"codelist1" #{"code1"}}})
      "valid response"
      "something wrong with filter parsing..."))

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
