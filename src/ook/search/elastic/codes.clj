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

(defn- build-code [{id :_id source :_source}]
  {:ook/uri id
   :label (:label source)
   :children (:narrower source)
   :used (-> source :used Boolean/parseBoolean)})

(defn- assoc-scheme [codelist-id code]
  (assoc code :scheme codelist-id))

(defn- build-codes
  "Map the codes from the elasticsearch result format to a more succinct format used internally.
  The scheme is passed in as an argument and not taken from (-> result :_source :scheme) because
  sometimes a code belongs to multiple schemes and that value is a collection.. It's included in
  the result in the first place so that each individual map contains all the information it
  needs to fetch its own children."
  [results codelist-id]
  (->> results
       :hits :hits
       (map build-code)
       (map (partial assoc-scheme codelist-id))
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
  (or (no-broader-codes conn codelist-id)
      (specified-top-concepts conn codelist-id)))

(declare find-narrower-concepts)

(defn- build-sub-tree [conn codelist-id code-uris]
  (let [codes (-> (get-codes* conn code-uris)
                  (build-codes codelist-id))]
    (doall
      (map (fn [code]
             (if (:children code)
               (find-narrower-concepts conn codelist-id code)
               code))
           codes))))

(defn- find-narrower-concepts [conn codelist-id {:keys [children] :as concept}]
  (if (seq children)
    (assoc concept :children (build-sub-tree conn codelist-id children))
    concept))

(defn build-concept-tree [codelist-id {:keys [elastic/endpoint]}]
  (if codelist-id
    (let [conn (esu/get-connection endpoint)]
      (doall
       (map (partial find-narrower-concepts conn codelist-id)
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
