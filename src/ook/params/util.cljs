(ns ook.params.util
  (:require [reitit.impl :as ri]
            [reitit.frontend :as rtfe]
            [cemerick.url :as url]
            [clojure.string :as str]))

(def base-uri "http://gss-data.org.uk/")

(defn encode-filter-facets [selection]
  (->> selection
       keys
       (map #(str/split % #","))
       (map (fn [[dim val]]
              (str (url/url-encode (str base-uri dim))
                   ","
                   (url/url-encode (str base-uri val)))))))

(defn link-to-pmd-dataset [id selection]
  (let [query-string (str (ri/query-string {:uri (str base-uri id)
                                            :filters-drawer "open"
                                            :filter-facets (encode-filter-facets selection)}))]
    (str "https://staging.gss-data.org.uk/cube/explore?" query-string)))
