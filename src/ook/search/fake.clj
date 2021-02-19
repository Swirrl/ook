(ns ook.search.fake
  (:require [integrant.core :as ig]
            [ook.search.db :as db]))

(defrecord FakeSearch []
  db/SearchBackend

  (get-codes [_ query]
    (condp = query
      "" {:result.codes/query ""
          :result.codes/count 0
          :result.codes/data []}
      "test" {:result.codes/query "test"
              :result.codes/count 1
              :result.codes/data [{:id "http://test"
                             :label "This is a test label"}]})))

(defmethod ig/init-key :ook.search.fake/db [_ _]
  (->FakeSearch))
