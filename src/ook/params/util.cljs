(ns ook.params.util
  (:require [reitit.impl :as ri]
            [cemerick.url :as url]
            [clojure.string :as str]))

(def base-uri "http://gss-data.org.uk/")
(def pmd-uri "https://beta.gss-data.org.uk/")

(defn- pair-dimensions-and-codes [{dim-uri :ook/uri codes :codes}]
  (when codes
    (map (fn [{code-uri :ook/uri}] [dim-uri code-uri]) codes)))

(defn absolute-uri [uri]
  (if (str/starts-with? uri "http")
    uri
    (str base-uri uri)))

(defn- encode-pmd-style [[dim val]]
  (str (url/url-encode (absolute-uri dim))
       ","
       (url/url-encode (absolute-uri val))))

(defn- remove-matches-where-codelist-applied
  "If the user specifies a codelist then the matching codes are just examples not criteria.
  This method removes these so that they don't feature in the PMD link."
  [applied-facets dataset-facets]
  (let [applied-codelist? (->> applied-facets
                               (mapcat val)
                               (filter (fn [[codelist codes]] (empty? codes)))
                               (map key)
                               set)]
    (map (fn [facet] (update facet :dimensions (partial map (fn [dimension] (update dimension :codes (partial remove (fn [code] (some applied-codelist? (map :ook/uri (:scheme code)))))))))) dataset-facets)))

(defn encode-filter-facets
  "Encodes filter facets in the form [dimension value] the same way
  as PMD so that they can be used to construct a link to a cube in pmd"
  [dataset-facets applied-facets]
  (->> dataset-facets
       (remove-matches-where-codelist-applied applied-facets)
       (mapcat :dimensions)
       (mapcat pair-dimensions-and-codes)
       (remove nil?)
       (map encode-pmd-style)))

(defn link-to-pmd-dataset [id dataset-facets applied-facets]
  (let [filter-facets (encode-filter-facets dataset-facets applied-facets)
        query-string (str (ri/query-string (cond-> {:uri (str base-uri id)}
                                             (seq filter-facets) (assoc :apply-filters true
                                                                        :filter-facets filter-facets))))]
    (str pmd-uri "cube/explore?" query-string)))
