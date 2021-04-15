(ns ook.params.util
  (:require [reitit.impl :as ri]
            [cemerick.url :as url]))

(def base-uri "http://gss-data.org.uk/")
(def pmd-uri "https://staging.gss-data.org.uk/")

(defn- pair-dimensions-and-codes [{dim-uri :ook/uri codelists :codelists}]
  (mapcat (fn [{:keys [examples]}]
            (mapcat (fn [{code-uri :ook/uri}]
                      [dim-uri code-uri])
                    examples))
          codelists))

(defn- encode-pmd-style [[dim val]]
  (str (url/url-encode (str base-uri dim))
       ","
       (url/url-encode (str base-uri val))))

(defn encode-filter-facets
  "Encodes filter facets in the form [dimension value] the same way
  as PMD so that they can be used to construct a link to a cube in pmd"
  [filter-facets]
  (->> filter-facets
       (mapcat :dimensions)
       (map pair-dimensions-and-codes)
       (map encode-pmd-style)))

(defn link-to-pmd-dataset [id filter-facets]
  (let [query-string (str (ri/query-string {:uri (str base-uri id)
                                            :apply-filters true
                                            :filter-facets (encode-filter-facets filter-facets)}))]
    (str pmd-uri "cube/explore?" query-string)))
