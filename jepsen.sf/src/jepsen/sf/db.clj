(ns jepsen.sf.db
  (:require [clojure.tools.logging :refer [info warn]]
            [clojure.string :as str]
            [jepsen.consul.client :as client]
            [jepsen.core :as jepsen]
            [jepsen.db :as db]
            [jepsen.control :as c]
            [jepsen.control.net :as net]
            [jepsen.control.util :as cu]
            [cheshire.core :as json]
            [slingshot.slingshot :refer [throw+ try+]]))

(def dir "/opt")
(def binary "consul")
(def config-file "/opt/consul.json")
;; TODO Condense these into `dir`, we don't need to sprawl these files in tests
(def pidfile "/var/run/consul.pid")
(def logfile "/var/log/consul.log")
(def data-dir "/var/lib/consul")

(def retry-interval "5s")

(defn start-sf!
  [test node]
  (info node "starting sf")
  (cu/start-daemon!
   {:logfile logfile
    :pidfile pidfile
    :chdir   dir}
   binary
   :agent
   :-server
   :-log-level "debug"
   :-client    "0.0.0.0"
   :-bind      (net/ip (name node))
   :-data-dir  data-dir
   :-node      (name node)
   :-retry-interval retry-interval
   ;; TODO Enable this when we're giving the system on-disk config
   #_(when (= node (jepsen/primary test))
     [:-config-file config-file])

   ;; Setup node in bootstrap mode if it resolves to primary
   (when (= node (jepsen/primary test)) :-bootstrap)

   ;; Join if not primary
   (when-not (= node (jepsen/primary test))
     [:-retry-join (net/ip (name (jepsen/primary test)))])

   ;; Shovel stdout to logfile
   :>> logfile
   (c/lit "2>&1")))

(defn db
  "prepare and cleanup test"
  []
  (reify db/DB
    (setup! [this test node]

            (info node "Configuring cluster" version)

            (info node "Cluster Configure" version)
            )

    (teardown! [_ test node]
     (info node "Cleaning up Cluster killed")

      (info node "Cluster Cleaned"))

    db/LogFiles
    (log-files [_ test node]
      [logfile])))
