(ns ook.spec
  (:require
   [clojure.spec.alpha :as s]))

(s/def :ook.spec/db (s/keys :req [:app/current-route
                                  :facets/config
                                  :datasets/count
                                  :results.datasets/data]

                            :opt [:facets/applied
                                  :ui.facets/current
                                  :ui.codes/status
                                  :ui.facets/status]))

;;;;;;; route

(s/def :app/current-route #(instance? reitit.core.Match %))

;;;;;;; status

(s/def :ui.codes/status (s/map-of :ook/uri :ook/async-loading-status))
(s/def :ui.facets/status (s/map-of :facet/name :ook/async-loading-status))

(s/def :ook/async-loading-status #{:ready :error :loading})


;;;;;;; facets


(s/def :facets/config (s/map-of :facet/name :ook/facet))

(s/def :ook/facet (s/keys :req-un [:facet/name
                                   :facet/sort-priority]
                          :opt-un [:facet/dimensions
                                   :facet/codelists]))

(s/def :facet/name string?)
(s/def :facet/sort-priority number?)
(s/def :facet/dimensions (s/coll-of string?))
(s/def :facet/codelists (s/or :no-codelists #{:no-codelists}
                              :codelists (s/map-of :ook/uri :facet/codelist)))
(s/def :facet/codelist (s/keys :req [:ook/uri]
                               :req-un [:ook/label]
                               :opt-un [:ook/children]))

(s/def :ook/children (s/or :no-children #{:no-children} :children (s/coll-of :ook/code)))
(s/def :ook/code (s/keys :req [:ook/uri]
                         :req-un [:ook/label
                                  :code/children
                                  :code/used
                                  :code/scheme]))
(s/def :code/children (s/nilable (s/coll-of :ook/code)))
(s/def :code/used boolean?)
(s/def :code/scheme string?)

(s/def :facet/selection (s/nilable (s/map-of :ook/uri (s/nilable (s/coll-of :ook/uri)))))
(s/def :ook/uri string?)

(s/def :facets/applied (s/nilable (s/map-of :facet/name :facet/selection)))

;;;;;;; current facet

(s/def :ui.facets/current (s/nilable (s/keys :req-un [:facet/name])))


;;;;;;; datasets


(s/def :datasets/count number?)

(s/def :results.datasets/data (s/or :state #{:loading :error} :results (s/coll-of :ook/dataset)))
(s/def :ook/dataset (s/keys :req [:ook/uri]
                            :req-un [:ook/label]
                            :opt-un  [:ook/matching-observation-count
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
                                       :dataset.facet/dimensions]))

(s/def :dataset.facet/dimensions (s/coll-of :dataset.facet/dimension))
(s/def :dataset.facet/dimension (s/keys :req [:ook/uri]
                                        :req-un [:ook/label]
                                        :opt-un [:facet/codes]))
(s/def :facet/codes (s/coll-of :facet/code))
(s/def :facet/code (s/keys :opt [:ook/uri]
                           :opt-un [:ook/label]))
