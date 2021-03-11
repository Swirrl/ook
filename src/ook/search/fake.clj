(ns ook.search.fake
  (:require [integrant.core :as ig]
            [ook.search.db :as db]))

(defrecord FakeSearch []
  db/SearchBackend

  (get-codes [_ query]
    (condp = query
      "" []
      "test" [{:id "http://test" :label "This is a test label"}]))

  (get-datasets [_ filters]
    (condp = filters
      nil []

      [{:id "a-code" :scheme "scheme-1"}] "valid response 1"

      [{:id "a-code", :scheme "scheme-1"} {:id "another-code", :scheme "scheme-2"}]
      "valid response 2"))

  (all-datasets [_]
    ["datasets..."]))

(defmethod ig/init-key :ook.search.fake/db [_ _]
  (->FakeSearch))
