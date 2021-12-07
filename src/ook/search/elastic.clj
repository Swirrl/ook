(ns ook.search.elastic
  (:require
   [ook.search.db :as db]
   [ook.search.elastic.datasets :as ds]
   [ook.search.elastic.codes :as codes]
   [ook.search.elastic.components :as components]
   [ook.search.elastic.facets :as facets]
   [ook.search.elastic.text :as text]
   [integrant.core :as ig]))

(defrecord Elasticsearch [opts]
  db/SearchBackend

  (get-codes [_ uris]
    (codes/get-codes uris opts))

  (search-codes [_ params]
    (codes/search params opts))

  (get-components [_ uris]
    (components/get-components uris opts))

  (components->codelists [_ component-uris]
    (components/components->codelists component-uris opts))

  (get-datasets-for-facets [_ facets]
    (ds/for-facets facets opts))

  (get-datasets-for-components [_ components]
    (ds/for-components components opts))

  (all-datasets [_]
    (ds/all opts))

  (dataset-count [_]
    (ds/total-count opts))

  (get-facets [_]
    (facets/get-facets opts))

  (get-concept-tree [_ codelist]
    (codes/build-concept-tree codelist opts))

  (search [_ query]
    (text/dataset-search query opts)))

(defmethod ig/init-key :ook.search.elastic/db [_ opts]
  (->Elasticsearch opts))
