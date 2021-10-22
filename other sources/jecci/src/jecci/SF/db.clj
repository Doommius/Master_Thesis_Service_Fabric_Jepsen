(ns jecci.SF.db
  "Tests for postgres"
  (:require [clojure.tools.logging :refer :all]
            [clojure.stacktrace :as cst]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [dom-top.core :refer [with-retry]]
            [fipp.edn :refer [pprint]]
            [jepsen [core :as jepsen]
             [control :as c]
             [db :as db]
             [faketime :as faketime]
             [util :as util]]
            [jepsen.control.util :as cu]
            [clojure.java.jdbc :as j]
            [jecci.utils.handy :as juh]
            [jecci.utils.db :as jud])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(def home "/home/jecci")
(def pg-dir "/home/jecci/postgresql")
(def pg-install-dir "/home/jecci/pginstall")
(def pg-bin-dir (str pg-install-dir "/bin"))
(def pg-postgres-file (str pg-bin-dir "/postgres"))
(def pg-initdb-file (str pg-bin-dir "/initdb"))
(def pg-pg_ctl-file (str pg-bin-dir "/pg_ctl"))
(def pg-psql-file (str pg-bin-dir "/psql"))
(def pg-pg_basebackup-file (str pg-bin-dir "/pg_basebackup"))
(def pg-pg_isready-file (str pg-bin-dir "/pg_isready"))

(def pg-data "/home/jecci/pgdata")
(def pg-config-file (str pg-data "/postgresql.conf"))
(def pg-hba-file (str pg-data "/pg_hba.conf"))
(def pg-log-file (str pg-data "/pg.log"))
(def pg_ctl-stdout (str pg-data "/pg_ctl.stdout"))

(def repeatable-read "shared_buffers=4GB\nlisten_addresses = '*'\nwal_keep_size=16GB\n
default_transaction_isolation = 'repeatable read'")
(def serializable "shared_buffers=4GB\nlisten_addresses = '*'\nwal_keep_size=16GB\n
default_transaction_isolation = 'serializable'")

; leader of pg cluster, should contain only one node
(def leaders ["10.0.0.4"])
(def backups ["n2" "n3" "n4" "n5"])

(defn running?
  "Is the service running?"
  []
  (try
    (c/exec :pgrep :-f :ReliableCollectionsWebAPI.dll)
    true
    (catch RuntimeException _ false)))


(defn start_sf!
  "Starts DB."
  [node test]
  (info "Deploying Service to Cluster")
  (info (running?) )
  (info "Deployed Service to Cluster"))


(defn stop_sf!
  "Stops DB."
  [node test]
  (info "kill instance of DB")
  (c/su
    (meh (c/exec :sudo :killall :-9 :-u :sfappsuser)))
  )

(defn unprevision!
  "teardown DB."
  [node test]
  (info "kill instance of service as the cluster itself stays running,")
  (c/su
    (meh (c/exec :sudo :killall :-9 :-u :sfappsuser)))
  (info "This should be updated to unprevision the service and clear any storage.")

  )

(defn get-leader-node
  "Get leader, should return one node"
  []
  (first leaders))

(defn isleader?
  [node]
  (contains? (into #{} leaders) node))


(defn install!
  "Downloads archive and extracts it to our local pg-dir, if it doesn't exist
  already. If test contains a :force-reinstall key, we always install a fresh
  copy.

  Calls `sync`"
  [test node]
  (c/su
   (c/exec "pkill" :-9 :-e :-c :-U "jecci" :-f "postgres" :|| :echo "no process to kill")
   (c/exec :rm :-rf pg-data)
   (c/exec :rm :-rf pg-dir))
  (when (or (:force-reinstall test)
            (not (cu/exists? pg-install-dir)))
    (info node "installing postgresql")
    (cu/install-archive! (tarball-url test) pg-dir {:user? "jecci", :pw? "123456"})
    
    (c/exec "mkdir" "-p" pg-data)
    (c/cd pg-dir
          (c/exec (str pg-dir "/configure") (str "--prefix=" pg-install-dir) "--enable-depend" "--enable-cassert"
                  "--enable-debug"  "--without-readline" "--without-zlib")
          (juh/exec->info  (c/exec "make" "-j8" "-s"))
          (juh/exec->info  (c/exec "make" "install" "-j8" "-s")))))

(defn db
  "postgres"
  []
  (reify db/DB
    (setup! [db test node]
      (info "setting up postgres")
      (info "not yet complete")
      (info "git clone github.com/doommius/thesis.git")
      (info "cd thesis/Jepsen")
      (info "deploy Dotnet application to cluster")
      (info "Await until clusters and nodes are ready.")
      (info "Deploying Service to Cluster")
      (info ((try
               (c/exec :pgrep :-f :ReliableCollectionsWebAPI.dll)
               true
               (catch RuntimeException _ false))) )
      (info "Deployed Service to Cluster")

      ;(info node "finish installing" (str (when (isleader? node) " and init for leaders")))
      (jepsen/synchronize test 180)

      (info "Figure out who is leader?")

      (info node "finish starting up")
      (jepsen/synchronize test 180))

    (teardown! [db test node]
        (unprevision! node test)
      (info node "finish tearing down"))

    ; Following files will be downloaded from nodes
    ; When test fail or finish
    db/LogFiles
    (log-files [_ test node]
      [pg-log-file])))
