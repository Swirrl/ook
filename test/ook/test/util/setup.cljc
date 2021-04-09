(ns ook.test.util.setup
  (:require
   [clojure.spec.alpha :as s]
   #?@(:clj [[clojure.java.io :as io]
             [ook.etl :as etl]
             [ook.index :as idx]
             [ook.search.elastic :as es]
             [integrant.core :as ig]
             [ook.concerns.integrant :as i]
             [vcr-clj.clj-http :refer [with-cassette]]
             [ook.main :as main]]
       :cljs [[reagent.dom :as rdom]
              [re-frame.core :as rf]])))

#?(:clj
   (do
     (def test-profiles
       (concat main/core-profiles
               [(io/resource "test.edn")
                (io/resource "project/fixture/facets.edn")]))

     (defn start-system! [profiles]
       (i/exec-config {:profiles profiles}))

     (def stop-system! ig/halt!)

     (defmacro with-system
       "Start a system with the given profiles"
       [[sym profiles] & body]
       `(let [~sym (start-system! ~profiles)]
          (try
            ~@body
            (finally
              (stop-system! ~sym)))))

     (def example-cubes
       ["http://gss-data.org.uk/data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-production#dataset"
        "http://gss-data.org.uk/data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-duty-receipts#dataset"
        "http://gss-data.org.uk/data/gss_data/trade/hmrc-alcohol-bulletin/alcohol-bulletin-clearances#dataset"])

     (defn example-datasets [system]
       (with-cassette :extract-datasets
         (let [query (slurp (io/resource "etl/dataset-construct.sparql"))]
           (etl/extract system query "qb" example-cubes))))

     (defn load-datasets! [system]
       (let [datasets (example-datasets system)
             frame (slurp (io/resource "etl/dataset-frame.json"))
             jsonld (etl/transform frame datasets)]
         (etl/load-documents system "dataset" jsonld)))

     (defn not-localhost? [req & _]
       (not (= (:server-name req)
               "localhost")))

     (defn reset-indicies! [system]
       (idx/delete-indicies system)
       (idx/create-indicies system))

     (defn load-fixtures! [system]
       (reset-indicies! system)

       (with-cassette {:name :fixtures :recordable? not-localhost?}
         (etl/pipeline system)))

     (defn get-db [system]
       (es/->Elasticsearch
        {:elastic/endpoint (:ook.concerns.elastic/endpoint system)
         :ook/facets (:ook.search/facets system)})))

   ;;;;; CLJS test setup

   :cljs
   (do
     (defn set-id [el id] (set! (.-id el) id))

     (def test-div (doto (.createElement js/document "div") (set-id "test-harness")))

     (def body (-> js/document (.getElementsByTagName "body") first))

     (defn init! [component-fn initial-state]
       ;; stub validation because reframe-test `run-test-sync` helper clears the
       ;; app db (sets it to {}) between each test run, which fails the validation
       (s/def :ook.spec/db map?)

       (.appendChild body test-div)
       (rf/dispatch [:init/initialize-db initial-state])
       (rdom/render [component-fn] test-div))

     (defn cleanup! []
       (.removeChild body test-div))))
