(ns jepsen.sf.db
  (:require [clojure.tools.logging :refer [info warn]]
            [clojure.string :as str]
            [jepsen.sf.client :as client]
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
(def logfile "var/log/sfnode/sfnodelog")
(def data-dir "/var/lib/consul")
(def retry-interval "5s")

(defn db
  "prepare and cleanup test"
  []
  (reify db/DB
    (setup! [this test node]

            (info node "Configuring cluster")

            (info node "Cluster Configure")
            )

    (teardown! [_ test node]
     (info node "Cleaning up Cluster killed")

      (info node "Cluster Cleaned"))

    db/LogFiles
    (log-files [_ test node]
      [logfile])))
