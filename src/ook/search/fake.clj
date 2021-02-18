(ns ook.search.fake
  (:require [integrant.core :as ig]
            [ook.search.db :as db]))

(defrecord FakeSearch []
  db/SearchBackend

  (get-codes [_ query]
    (condp = query
      "" {:result/query ""
          :result/count 0
          :result/data []}
      "test" {:result/query "test"
              :result/count 1
              :result/data [{:id "http://test"
                             :label "This is a test label"}]})))

(defmethod ig/init-key :ook.search.fake/db [_ _]
  (->FakeSearch))
