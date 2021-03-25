(ns ook.params.util
  (:require [reitit.impl :as ri]
            [clojure.string :as str]
            [cemerick.url :as url]))

(def base-uri "http://gss-data.org.uk/")
(def pmd-uri "https://staging.gss-data.org.uk/")

(defn encode-filter-facets
  "Encodes filter facets in the form [dimension value] the same way
  as PMD so that they can be used to construct a link to a cube in pmd"
  [filter-facets]
  (map (fn [{:keys [dimension value]}]
         (str (url/url-encode (str base-uri dimension))
              ","
              (url/url-encode (str base-uri value))))
       filter-facets))

(defn link-to-pmd-dataset [id filter-facets]
  (let [query-string (str (ri/query-string {:uri (str base-uri id)
                                            :filters-drawer "open"
                                            :filter-facets (encode-filter-facets filter-facets)}))]
    (str pmd-uri "cube/explore?" query-string)))

(defn encode-facet
  "Encodes a pre-configured filter facet in the form [facet-name & selected-codelists]
  in a way that the ook server understands and can decode for db searching"
  [facet]
  (str/join "," (map url/url-encode facet)))
