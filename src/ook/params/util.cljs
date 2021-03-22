(ns ook.params.util
  (:require [reitit.impl :as ri]
            [cemerick.url :as url]))

(def base-uri "http://gss-data.org.uk/")
(def pmd-uri "https://staging.gss-data.org.uk/")

(defn encode-filter-facets [filter-facets]
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
