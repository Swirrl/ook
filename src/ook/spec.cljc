(ns ook.spec
  (:require
   [clojure.spec.alpha :as s]))

(s/def :ook.spec/db (s/keys :req [:app/current-route
                                  :facets/config
                                  :datasets/count
                                  :results.datasets/data]

                            :opt [:facets/applied
                                  :ui.facets/current]))

;;;;;;; route

(s/def :app/current-route #(instance? reitit.core.Match %))

;;;;;;; facets

(s/def :facets/config (s/map-of :facet/name :ook/facet))

(s/def :ook/facet (s/keys :req-un [:facet/name]
                          :opt-un [:facet/selection]))
(s/def :facet/name string?)
(s/def :facet/selection (s/nilable (s/map-of :ook/uri (s/nilable (s/coll-of :ook/uri)))))
(s/def :ook/uri string?)

(s/def :facet/codelist (s/keys :req [:ook/uri] :req-un [:ook/label]))

(s/def :facets/applied (s/nilable (s/map-of :facet/name :facet/selection)))
(s/def :ook/current-facet-state #{:loading :error})
(s/def :ui.facets/current (s/nilable (s/or :facet :ook/facet :state :ook/current-facet-state)))

;;;;;;; datasets

(s/def :datasets/count number?)

(s/def :results.datasets/data (s/or :state #{:loading :error} :results (s/coll-of :ook/dataset)))
(s/def :ook/dataset (s/keys :req [:ook/uri]
                            :opt-un [:ook/label
                                     :ook/matching-observation-count
                                     :ook/comment
                                     :ook/description
                                     :ook/publisher
                                     :dataset/facets]))

(s/def :ook/label string?)
(s/def :ook/comment string?)
(s/def :ook/description string?)
(s/def :ook/matching-observations number?)
(s/def :ook/publisher (s/keys :req-un [:publisher/altlabel]))
(s/def :publisher/altlabel string?)

(s/def :dataset/facets (s/coll-of :dataset/facet))
(s/def :dataset/facet (s/keys :req-un [:facet/name
                                       :facet/dimensions]))

(s/def :facet/name string?)
(s/def :facet/dimensions (s/coll-of :facet/dimension))
(s/def :facet/dimension (s/keys :req [:ook/uri]
                                :req-un [:ook/label]
                                :opt-un [:facet/codes]))
(s/def :facet/codes (s/coll-of :facet/code))
(s/def :facet/code (s/keys :opt [:ook/uri]
                           :opt-un [:ook/label]))

(s/def :results.datasets/error map?)
