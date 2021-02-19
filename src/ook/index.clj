(ns ook.index
  (:require [clojurewerkz.elastisch.rest :as es]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojure.data.json :as json]
            [clojure.java.io :as io]))

(defn connect [endpoint]
  (es/connect endpoint {:content-type :json}))

(defn create-index [{:keys [:ook.concerns.elastic/endpoint] :as system} index mapping-file]
  (esi/create (connect endpoint) index (-> mapping-file io/reader json/read)))

(defn delete-index [{:keys [:ook.concerns.elastic/endpoint] :as system} index]
  (esi/delete (connect endpoint) index))

(defn each-index [f]
  (let [indicies ["dataset" "component" "code" "observation"]]
    (zipmap (map keyword indicies)
            (map f indicies))))

(defn create-indicies [system]
  (each-index #(create-index system % (io/resource (str "etl/" % "-mapping.json")))))

(defn delete-indicies [system]
  (each-index (partial delete-index system)))
