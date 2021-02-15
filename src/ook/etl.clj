(ns ook.etl
  (:require
   [drafter-client.client :as dc]
   [drafter-client.client.impl :as dci]
   [grafter-2.rdf4j.repository :as gr-repo]))

(defn extract-datasets [{:keys [drafter-client/client] :as system}]
  (let [repo (dc/->repo client nil dc/live)]
    (with-open [conn (gr-repo/->connection repo)]
      (doall (gr-repo/query conn "SELECT * WHERE { ?s ?p ?o } LIMIT 10")))))
