(ns ook.concerns.integrant
  "This namespace defines various integrant concerns, e.g.
    data-readers and derived constant keys etc.

  This file should be explicitly required by entry points to the
  app."
  (:require [integrant.core :as ig]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as st]
            [clojure.tools.logging :as log]
            [meta-merge.core :as mm]))

;; init of constants is a no-op
(defmethod ig/init-key :ook/const [_ v] v)

;; (derive :ook/some-const :ook/const) for constants
(derive :auth0.client/id :ook/const)
(derive :auth0.client/secret :ook/const)
(derive :auth0.client/endpoint :ook/const)
(derive :auth0.client/api :ook/const)
(derive :drafter/endpoint-url :ook/const)
(derive :ook.concerns.elastic/endpoint :ook/const)

(defn env [[env-var default]]
  (or (System/getenv env-var) default))

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
      (when-let [r (str (io/file (io/resource (str "secrets/" variable ".gpg"))))] (decrypt r))
      (if (nil? default)
        (throw (Throwable. (str "Couldn't find configuration for " variable)))
        default)))

(defn load-config [profile]
  (->> (io/resource profile)
       slurp
       (ig/read-string {:readers {'env env
                                  'secret secret
                                  'resource io/resource}})))

(defn config [profiles]
  (apply mm/meta-merge (map load-config profiles)))

(defn exec-config [{:keys [profiles] :as opts}]
  (ig/init (doto (config profiles) (ig/load-namespaces))))
