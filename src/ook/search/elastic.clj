(ns ook.search.elastic
  (:require
   [ook.search.db :as db]
   [ook.search.elastic.datasets :as ds]
   [ook.search.elastic.codes :as codes]
   [ook.search.elastic.components :as components]
   [ook.search.elastic.facets :as facets]
   [integrant.core :as ig]))

(defrecord Elasticsearch [opts]
  db/SearchBackend

  (get-codes [_ query]
    (codes/search query opts))

  (get-components [_ uris]
    (components/get-components uris opts))

  (components->codelists [_ component-uris]
    (components/components->codelists component-uris opts))

  ;; (get-datasets [_ filters]
  ;;   (ds/apply-filter filters opts))

  (get-datasets-for-facets [_ facets]
    (ds/for-facets facets opts))

  (get-datasets-for-components [_ components]
    (ds/for-components components opts))

  (all-datasets [_]
    (ds/all opts))

  (dataset-count [_]
    (ds/total-count opts))

  (get-facets [_]
    (facets/get-facets opts)))

(defmethod ig/init-key :ook.search.elastic/db [_ opts]
  (->Elasticsearch opts))
