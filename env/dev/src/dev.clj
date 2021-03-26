(ns dev
  (:require [ook.main :as main]
            [integrant.repl :as igr :refer [go reset halt]]
            [integrant.repl.state :refer [system config]]
            [clojure.tools.namespace.repl :as tns]
            [clojure.java.io :as io]))

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
