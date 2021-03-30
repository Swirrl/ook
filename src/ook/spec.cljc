(ns ook.spec
  (:require
   [clojure.spec.alpha :as s]))

(s/def :ook.spec/db (s/keys :req [:app/current-route
                                  :facets/config
                                  :datasets/count
                                  (or :results.datasets/data :results.datasets/error)]

                            :opt [:facets/applied
                                  :ui.facets/current]))

;;;;;;; route

(s/def :app/current-route #(instance? reitit.core.Match %))

;;;;;;; facets

(s/def :facets/config (s/coll-of :ook/facet))

(s/def :ook/facet (s/keys :req-un [:facet/name :facet/codelists]
                          :opt-un [:facet/selection]))
(s/def :facet/name string?)
(s/def :facet/selection (s/coll-of :ook/uri))
(s/def :ook/uri string?)

(s/def :facet/codelists (s/coll-of :facet/codelist))
(s/def :facet/codelist (s/keys :req [:ook/uri] :req-un [:ook/label]))

(s/def :facets/applied (s/nilable (s/map-of :facet/name :facet/selection)))
(s/def :ui.facets/current (s/nilable :ook/facet))

;;;;;;; datasets

(s/def :datasets/count number?)

(s/def :results.datasets/data (s/coll-of :ook/dataset))
(s/def :ook/dataset (s/keys :req [:ook/uri]
                            :req-un [:ook/label]
                            :opt-un [:ook/matching-observations
                                     :ook/comment
                                     :dataset/facets]))

(s/def :ook/label string?)
(s/def :ook/comment string?)
(s/def :ook/matching-observations number?)

(s/def :dataset/facets (s/coll-of :dataset/facet))
(s/def :dataset/facet (s/keys :req-un [:facet/name
                                       :facet/dimensions]))

(s/def :facet/name string?)
(s/def :facet/dimensions (s/coll-of :facet/dimension))
(s/def :facet/dimension (s/keys :req [:ook/uri]
                                :opt-un [:facet/codelist]))

(s/def :results.datasets/error map?)
