(ns ook.index
  (:require [clojurewerkz.elastisch.rest :as es]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [ook.concerns.integrant :as i]
            [ook.etl :as etl]
            [clojure.tools.logging :as log]))

(defn connect [endpoint]
  (es/connect endpoint {:content-type :json}))


(defn create-index
  ([system index]
   (create-index system index (io/resource (str "etl/" index "-mapping.json"))))
  ([{:keys [:ook.concerns.elastic/endpoint] :as system} index mapping-file]
   (esi/create (connect endpoint) index {:mappings (get (-> mapping-file io/reader json/read) "mappings")})))

(defn delete-index [{:keys [:ook.concerns.elastic/endpoint] :as system} index]
  (esi/delete (connect endpoint) index))

(defn update-settings [{:keys [:ook.concerns.elastic/endpoint] :as system} index settings]
  (esi/update-settings (connect endpoint) index settings))

(defn get-mapping [{:keys [:ook.concerns.elastic/endpoint] :as system} index]
  (esi/get-mapping (connect endpoint) index))


(defn each-index [f]
  (let [indicies ["dataset" "component" "code" "observation"]]
    (zipmap (map keyword indicies)
            (map f indicies))))


(defn create-indicies [system]
  (log/info "Creating indicies")
  (each-index (partial create-index system)))

(defn delete-indicies [system]
  (log/info "Deleting indicies")
  (each-index (partial delete-index system)))


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
  (delete-indicies system) ;; todo, make this "ensure indicies"
  (create-indicies system)
  (bulk-mode system)
  (let [result (etl/pipeline system)]
    (normal-mode system)
    result))

(defn -main
  "CLI Entry point for populating the index
  The targeting may be overriden by passing integrant profiles
  (specified as resource filename(s))"
  [& args]
  (let [loader ["drafter-client.edn"
                "elasticsearch-prod.edn"
                "load-data.edn"]
        target (or args ["project/trade/data.edn"
                         "cogs-staging.edn"])
        profiles (concat loader target)]
    (println "Starting index loader with profiles: " profiles)
    (i/exec-config {:profiles profiles})))
