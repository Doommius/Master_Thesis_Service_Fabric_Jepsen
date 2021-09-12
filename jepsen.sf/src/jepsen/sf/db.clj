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

(def dir "/mnt/sfroot/_App/JepsenType_App1/JepsenAPIStorePkg.Code.1.0.0/")
(def binary "JepsenAPIStore")
(def config-file "/mnt/sfroot/_App/JepsenType_App1/JepsenAPIStorePkg.Config.1.0.0/Settings.xml")
;; TODO Condense these into `dir`, we don't need to sprawl these files in tests
(def logfile "var/log/sfnode/sfnodelog")
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
     (info node "Cleaning up Cluster")

      (info node "Cluster Cleaned"))

    db/LogFiles
    (log-files [_ test node]
      [logfile])))
