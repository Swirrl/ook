(ns ook.params.util
  (:require [reitit.impl :as ri]
            [cemerick.url :as url]
            [clojure.string :as str]))

(def base-uri "http://gss-data.org.uk/")
(def pmd-uri "https://beta.gss-data.org.uk/")

(defn- pair-dimensions-and-codes [{dim-uri :ook/uri codes :codes}]
  (when codes
    (mapcat (fn [{code-uri :ook/uri}] [dim-uri code-uri]) codes)))

(defn absolute-uri [uri]
  (if (str/starts-with? uri "http")
    uri
    (str base-uri uri)))

(defn- encode-pmd-style [[dim val]]
  (str (url/url-encode (absolute-uri dim))
       ","
       (url/url-encode (absolute-uri val))))

(defn encode-filter-facets
  "Encodes filter facets in the form [dimension value] the same way
  as PMD so that they can be used to construct a link to a cube in pmd"
  [filter-facets]
  (->> filter-facets
       (mapcat :dimensions)
       (map pair-dimensions-and-codes)
       (remove nil?)
       (map encode-pmd-style)))

(defn link-to-pmd-dataset [id filter-facets]
  (let [filter-facets (encode-filter-facets filter-facets)
        query-string (str (ri/query-string (cond-> {:uri (str base-uri id) :apply-filters true}
                                             (seq filter-facets) (assoc :filter-facets filter-facets))))]
    (str pmd-uri "cube/explore?" query-string)))
