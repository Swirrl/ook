(ns ook.index
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [clojurewerkz.elastisch.rest.index :as esi]
   [integrant.core :as ig]
   [ook.concerns.integrant :as i]))

(defn create
  ([system index]
   (create system index (io/resource (str "etl/" index "-mapping.json"))))
  ([system index mapping-file]
   (esi/create (:ook.concerns.elastic/conn system)
               index
               {:mappings (get (-> mapping-file io/reader json/read) "mappings")
                :settings
                {:analysis
                 {:analyzer
                  {:ook_std
                   {:tokenizer "standard"
                    :filter ["lowercase" "stop" "stemmer"]}}}}})))

(defn delete [system index]
  (esi/delete (:ook.concerns.elastic/conn system) index))

(defn update-settings [{:keys [:ook.concerns.elastic/conn] :as system} index settings]
  (esi/update-settings conn index settings))

(defn get-mapping [{:keys [:ook.concerns.elastic/conn] :as system} index]
  (esi/get-mapping conn index))

(defn each [f]
  (let [indicies ["dataset" "component" "code" "observation" "graph"]]
    (zipmap (map keyword indicies)
            (map f indicies))))

(defn create-indicies [system]
  (log/info "Creating indicies")
  (each #(create system %)))

(defn delete-indicies [system]
  (log/info "Deleting indicies")
  (each #(delete system %)))

(defn bulk-mode [system]
  (log/info "Configuring indicies for load")
  (each #(update-settings system % {"index.refresh_interval" "-1"
                                    "index.number_of_replicas" "0"})))

(defn normal-mode [system]
  (log/info "Configuring indicies for search")
  (each #(update-settings system % {"index.refresh_interval" nil
                                    "index.number_of_replicas" "1"})))

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
