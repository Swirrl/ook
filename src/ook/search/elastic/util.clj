(ns ook.search.elastic.util
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojurewerkz.elastisch.rest :as esr]
   [clojurewerkz.elastisch.rest.document :as esrd]))

(defn get-connection [endpoint]
  (esr/connect endpoint {:content-type :json}))

(defn- replace-problematic-char [k]
  (if (= (keyword "@id") k)
    :ook/uri
    (keyword (str/replace k ":@" "ook/"))))

(defn normalize-keys [m]
  (let [remove-at (fn [[k v]]
                    (if (str/includes? (str k) "@")
                      [(replace-problematic-char k) v]
                      [k v]))]
    ;; only apply to maps
    (walk/postwalk (fn [x] (if (map? x) (into {} (map remove-at x)) x))
                   m)))

(defn all-hits
  "Searches with pagination to retreive more than 10,000 results.
  Returns as a lazy-seq of the docs (i.e. :hits of :hits)
  The sort key defaults to :_doc (index order) and size to 10,000.
  Doesn't save a Point-In-Time (to preserve index state over searches) because we're not expecting refreshes.
  https://www.elastic.co/guide/en/elasticsearch/reference/7.12/paginate-search-results.html#search-after"
  [conn index query]
  (let [query (-> query
                  (update :sort #(if (nil? %) :_doc %))
                  (update :size #(if (nil? %) 10000 %)))]
    (letfn [(get-page [cursor]
              (let [page-query (if cursor
                                 (assoc query :search_after cursor)
                                 query)]
                (let [page-result (get-in (esrd/search conn index "_doc" page-query)
                                          [:hits :hits])]
                  (if (= (:size page-query) (count page-result))
                    (concat page-result (get-page (-> page-result last :sort)))
                    page-result))))]
      (get-page nil))))
