(ns ook.test.util.setup
  (:require
   [clojure.spec.alpha :as s]
   #?@(:clj [[clojure.java.io :as io]
             [ook.etl :as etl]
             [ook.index :as idx]
             [ook.search.elastic :as es]
             [ook.search.elastic.util :as esu]
             [integrant.core :as ig]
             [ook.concerns.integrant :as i]
             [vcr-clj.clj-http :refer [with-cassette]]
             [ook.main :as main]]
       :cljs [[reagent.dom :as rdom]
              [re-frame.core :as rf]
              [ook.reframe.router :as router]
              [ook.params.parse :as p]
              [day8.re-frame.async-flow-fx]
              [ook.reframe.events]
              [ook.reframe.subs]
              [ook.reframe.facets.events]
              [ook.reframe.codes.events]
              [ook.reframe.codes.search.events]
              [ook.reframe.datasets.events]])))

#?(:clj
   (do
     (def test-profiles
       (concat main/core-profiles
               [(io/resource "test.edn")
                (io/resource "project/fixture/facets.edn")]))

     ;; TODO lot of duplication between here and dev?
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

     (defn not-localhost? [req & _]
       (not (= (:server-name req)
               "localhost")))

     (defn reset-indicies! [system]
       (idx/delete-indicies system)
       (idx/create-indicies system))

     (defn load-fixtures! [system]
       (reset-indicies! system)

       (with-cassette {:name :fixtures :recordable? not-localhost?}
         (etl/all-pipelines system)))

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

       (rf/dispatch-sync [:init/initialize-db initial-state])
       (router/home-controller)

       (.appendChild body test-div)
       (rdom/render [component-fn] test-div))

     (defn stub-dispatch-later []
       (rf/reg-fx
        :dispatch-later
        (fn [{:keys [dispatch]}]
          (rf/dispatch dispatch))))

     (def codelist-request (atom nil))

     (defn stub-codelist-fetch-success [codelists]
       (rf/reg-event-fx
        :http/fetch-codelists
        (fn [_ [_ name]]
          (reset! codelist-request name)
          {:dispatch [:http.codelists/success name (get codelists name)]})))

     (def concept-tree-request (atom nil))

     (defn stub-code-fetch-success [concept-trees]
       (rf/reg-event-fx
        :http/fetch-codes
        (fn [_ [_ facet codelist-uri]]
          (reset! concept-tree-request codelist-uri)
          {:dispatch [:http.codes/success facet codelist-uri (get concept-trees codelist-uri)]})))

     (def dataset-request (atom nil))

     (defn stub-dataset-fetch-success [datasets]
       (rf/reg-event-fx
        :http/fetch-datasets
        (fn [_ [_ filters]]
          (reset! dataset-request (p/deserialize-filter-state filters))
          {:dispatch [:http.datasets/success (get datasets (p/deserialize-filter-state filters) [])]})))

     (def last-navigation (atom nil))

     (defn stub-navigation
       "Manually trigger the controller for the matched route, which reitit would normally do"
       []
       (rf/reg-fx
        :app/navigate!
        (fn [{:keys [route query]}]
          (reset! last-navigation [route query])
          (condp = route
            :ook.route/home (router/home-controller)
            :ook.route/search (router/search-controller {:query query})))))

     (defn stub-code-search [search-results]
       (rf/reg-event-fx
        :http/search-codes
        (fn [_ [_ {:keys [codes/search name]}]]
          {:dispatch [:http.codes.search/success name (get search-results (:search-term search) [])]})))

     (defn stub-side-effects [{:keys [concept-trees codelists datasets search-results]}]
       (stub-navigation)
       (stub-dispatch-later)
       (stub-code-search (or search-results {}))
       (stub-dataset-fetch-success (or datasets {}))
       (stub-codelist-fetch-success (or codelists {}))
       (stub-code-fetch-success (or concept-trees {})))

     (defn cleanup! []
       (reset! codelist-request nil)
       (reset! concept-tree-request nil)
       (reset! dataset-request nil)
       (reset! last-navigation nil)
       (.removeChild body test-div))))
