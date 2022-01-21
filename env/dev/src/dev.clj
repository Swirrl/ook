(ns dev
  (:require
   [clojure.java.io :as io]
   [clojure.tools.namespace.repl :as tns]
   [integrant.core :as ig]
   [integrant.repl :as igr :refer [go reset halt]]
   [integrant.repl.state :refer [system config]]
   [ook.concerns.integrant :as i]
   [ook.main :as main]
   [ook.search.elastic.util :as esu]))

;; only automatically refresh/require project directories. Without
;; this tools.namespace (used by igr/reset) will load all namespaces
;; in gitlib dependencies, and load namespaces that require extra
;; dependencies.
(tns/set-refresh-dirs "src" "test" "env/dev/src")


;; require scope capture as a side effect
(require 'sc.api)

(def profiles (concat main/core-profiles
                      [(io/resource "dev.edn")
                       (io/resource "local.edn")]))



(igr/set-prep!
  #(do
     ;; notice this uses the main meta-config, but we just rely
     ;; on the right configs being on the resource path for the alias
     (main/prep-config {:profiles profiles})))



(defn start-system! [profiles]
  (let [system (i/exec-config {:profiles profiles})]
    (assoc system :es-conn (esu/get-connection
                            (:ook.concerns.elastic/endpoint system)))))

(def stop-system! ig/halt!)

(defmacro with-system
  "Start a system with the given profiles"
  [[sym profiles] & body]
  `(let [~sym (start-system! ~profiles)]
     (try
       ~@body
       (finally
         (stop-system! ~sym)))))
