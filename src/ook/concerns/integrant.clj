(ns ook.concerns.integrant
  "This namespace defines various integrant concerns, e.g.
    data-readers and derived constant keys etc.

  This file should be explicitly required by entry points to the
  app."
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as st]
   [clojure.tools.logging :as log]
   [clojurewerkz.elastisch.rest :as esr]
   [integrant.core :as ig]
   [meta-merge.core :as mm]))

;; init of constants is a no-op
(defmethod ig/init-key :ook/const [_ v] v)

(derive :auth0.client/id :ook/const)
(derive :auth0.client/secret :ook/const)
(derive :auth0.client/endpoint :ook/const)
(derive :auth0.client/api :ook/const)
(derive :drafter/endpoint-url :ook/const)
(derive :ook.search/facets :ook/const)
(derive :ook.assets/root :ook/const)

(defmethod ig/init-key :ook.concerns.elastic/conn [_ opts]
  (esr/connect (:endpoint opts) {:content-type :json}))

(defn env
  "Reader to lookup an env-var. If the default is an integer, the env-var's value
  will be coerced."
  [[env-var default]]
  (let [e (System/getenv env-var)]
    (if e
      (if (int? default)
        (Integer/parseInt e)
        e)
      default)))

(defn decrypt [filename]
  "Decrypts a file and returns a string with the contents (or nil on failure)"
  (let [{:keys [exit out err]} (shell/sh "gpg" "-d" filename)]
    (if (= exit 0)
      (st/trim-newline out)
      (log/warn "Failed to decrypt " filename ": " err))))

(defn secret [[variable default]]
  "Attempts to find a value in an environmental variable, an encrypted file, or a resorts to a default.
   The encrypted file should be a resource named e.g. secrets/MY_VAR.gpg"
  (or (System/getenv variable)
      (try
        (when-let [filename (io/file (io/resource (str "secrets/" variable ".gpg")))]
          (decrypt (str filename)))
        (catch Throwable e
          (log/warn "Failed to decrypt" variable "because" (.getMessage e))))
      (if (nil? default)
        (throw (Throwable. (str "Couldn't find configuration for " variable)))
        default)))

(defn load-config [profile]
  (->> (if (string? profile) (io/resource profile) profile)
       slurp
       (ig/read-string {:readers {'env env
                                  'secret secret
                                  'resource io/resource}})))

(defn config [profiles]
  (->> profiles
       (remove nil?)
       (map load-config)
       (apply mm/meta-merge)))

(defn exec-config [{:keys [profiles] :as opts}]
  (ig/init (doto (config profiles) (ig/load-namespaces))))
