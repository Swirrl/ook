(ns ook.search.elastic
  (:require
   [ook.search.db :as db]
   [ook.search.elastic.datasets :as ds]
   [ook.search.elastic.codes :as codes]
   [integrant.core :as ig]))

(defrecord Elasticsearch [opts]
  db/SearchBackend

  (get-codes [_ query]
    (codes/search query opts))

  (get-datasets [_ filters]
    (ds/apply-filter filters opts))

  (all-datasets [_]
    (ds/all opts)))

(defmethod ig/init-key :ook.search.elastic/db [_ opts]
  (->Elasticsearch opts))
