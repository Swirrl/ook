(ns ook.index
  (:require [clojurewerkz.elastisch.rest :as es]
            [clojurewerkz.elastisch.rest.index :as esi]
            [integrant.core :as ig]
            [ook.concerns.integrant :as i]
            [ook.etl :as etl]
            [clojure.tools.logging :as log]
            [ook.search.elastic.util :as esu]))

(defn update-settings [{:keys [:ook.concerns.elastic/endpoint] :as system} index settings]
  (esi/update-settings (esu/get-connection endpoint) index settings))

(defn get-mapping [{:keys [:ook.concerns.elastic/endpoint] :as system} index]
  (esi/get-mapping (esu/get-connection endpoint) index))

(defn each-index [f]
  (let [indicies ["dataset" "component" "code" "observation" "graph"]]
    (zipmap (map keyword indicies)
            (map f indicies))))

(defn create-indicies [system]
  (log/info "Creating indicies")
  (each-index (partial etl/create-index system)))

(defn delete-indicies [system]
  (log/info "Deleting indicies")
  (each-index (partial etl/delete-index system)))

(defn bulk-mode [system]
  (log/info "Configuring indicies for load")
  (each-index #(update-settings system % {"index.refresh_interval" "-1"
                                          "index.number_of_replicas" "0"})))

(defn normal-mode [system]
  (log/info "Configuring indicies for search")
  (each-index #(update-settings system % {"index.refresh_interval" nil
                                          "index.number_of_replicas" "1"})))

;; Loads an index with the configured content
(defmethod ig/init-key ::data [_ system]
  (bulk-mode system)
  (etl/with-deferred (normal-mode system)
    (etl/all-pipelines system)))

(defn -main
  "CLI Entry point for populating the index
  The targeting may be overriden by passing integrant profiles
  (specified as resource filename(s))"
  [& args]
  (let [loader ["drafter-client.edn"
                "elasticsearch-prod.edn"
                "load-data.edn"]
        target (or args ["project/all/data.edn"
                         "idp-beta.edn"])
        profiles (concat loader target)]
    (println "Starting index loader with profiles: " profiles)
    (i/exec-config {:profiles profiles})))
