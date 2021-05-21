(ns ook.search.elastic.codes
  (:require
   [ook.util :as u]
   [clojurewerkz.elastisch.query :as q]
   [ook.search.elastic.util :as esu]
   [clojurewerkz.elastisch.rest.document :as esd]))

(defn get-codes* [conn uris]
  (esd/search conn "code" "_doc"
              {:query (q/ids "_doc" uris)
               :size (count uris)}))

(defn get-codes
  "Find codes using their URIs."
  [uris {:keys [elastic/endpoint]}]
  (let [conn (esu/get-connection endpoint)
        uris (u/box uris)]
    (->> (get-codes* conn uris)
         :hits :hits
         (map :_source)
         (map esu/normalize-keys))))

(defn lift-coll
  "Lifts value to collection. Nil becomes an empty collection."
  [x]
  (if x (u/box x) (list)))

(defn build-code [scheme-uri doc]
  (-> doc
      (select-keys [:ook/uri :label])
      (assoc :children (lift-coll (:narrower doc)))
      (assoc :used (Boolean/parseBoolean (:used doc)))
      (assoc :scheme scheme-uri)))

(defn get-codes-in-scheme
  "Find codes using the scheme URI"
  [scheme-uri {:keys [elastic/endpoint]}]
  (let [conn (esu/get-connection endpoint)]
    (->> (esd/search conn "code" "_doc"
                     {:query {:term {:scheme scheme-uri}}
                      :size 10000}) ;; TODO paginate
         :hits :hits
         (map :_source)
         (map esu/normalize-keys)
         (map (partial build-code scheme-uri)))))

(defn- build-codes
  "Map the codes from the elasticsearch result format to a more succinct format used internally.
  The scheme is passed in as an argument and not taken from (-> result :_source :scheme) because
  sometimes a code belongs to multiple schemes and that value is a collection.. It's included in
  the result in the first place so that each individual map contains all the information it
  needs to fetch its own children."
  [results codelist-id]
  (->> results
       :hits :hits
       (map :_source)
       (map esu/normalize-keys)
       (map (partial build-code codelist-id))
       seq))

(defn- no-broader-codes [conn codelist-id]
  (-> conn
      (esd/search "code" "_doc"
                  {:size 5000
                   :query
                   {:bool
                    {:must {:term {:scheme codelist-id}}
                     :must_not {:exists {:field :broader}}}}})
      (build-codes codelist-id)))

(defn- specified-top-concepts [conn codelist-id]
  (-> conn
      (esd/search  "code" "_doc"
                   {:size 5000
                    :query
                    {:bool
                     {:must [{:term {:scheme codelist-id}}
                             {:term {:topConceptOf codelist-id}}]}}})
      (build-codes codelist-id)))

(defn get-top-concepts [conn codelist-id]
  (or (specified-top-concepts conn codelist-id)
      (no-broader-codes conn codelist-id)))

(declare find-narrower-concepts)

(defn- build-sub-tree [code-lookup child-uris]
  (let [children (->> child-uris (map code-lookup) (remove nil?))] ;; can be nil if child not in scheme (since children are merged across schemes)
    (doall
     (map (fn [code]
            (if (:children code)
              (find-narrower-concepts code-lookup code)
              code))
          children))))

(defn- find-narrower-concepts [code-lookup {:keys [children] :as concept}]
  (if (seq children)
    (assoc concept :children (build-sub-tree code-lookup children))
    concept))

(defn build-concept-tree [codelist-id {:keys [elastic/endpoint] :as opts}]
  (if codelist-id
    (let [conn (esu/get-connection endpoint)
          code-lookup (u/id-lookup (get-codes-in-scheme codelist-id opts))]
      (doall
       (map (partial find-narrower-concepts code-lookup)
            (get-top-concepts conn codelist-id))))
    []))

(def search-limit 10000)

(defn- search-codes [conn {:keys [search-term codelists]}]
  (esd/search conn "code" "_doc"
              {:size search-limit
               :query
               {:bool
                {:must [{:match {:label {:query search-term
                                         ;; interpret "world total" as "world AND total" not "world OR total"
                                         :operator "AND"}}}
                        {:terms {:scheme codelists}}]}}}))

(defn build-code-for-each-scheme [codelists {id :_id source :_source}]
  (let [code {:ook/uri id
              :used (-> source :used Boolean/parseBoolean)}
        schemes (-> source :scheme u/box set)]
    (->> schemes
         (filter codelists)
         (map (partial assoc code :scheme)))))

(defn search [{:keys [codelists] :as params} {:keys [elastic/endpoint]}]
  (let [conn (esu/get-connection endpoint)]
    (->> params
         (search-codes conn)
         :hits :hits
         (mapcat (partial build-code-for-each-scheme (set codelists))))))
