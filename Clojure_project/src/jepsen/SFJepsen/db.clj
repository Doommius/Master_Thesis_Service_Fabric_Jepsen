(ns jepsen.SFJepsen.db
  (:require [clojure [pprint :refer :all]
             [string :as str]]
            [clojure.java.io :as io]
            [clojure.tools.logging :refer [debug info warn]]
            [jepsen [core :as jepsen]
             [db :as jdb]
             [cli :as cli]
             [util :as util :refer [meh
                                    timeout
                                    relative-time-nanos]]
             [control :as c :refer [|]]
             [client :as client]
             [checker :as checker]
             [generator :as gen]
             [nemesis :as nemesis]
             [store :as store]
             [report :as report]
             [tests :as tests]]
            [jepsen.control
             [net :as net]
             [util :as cu]]
            [jepsen.os.debian :as debian]
            [knossos.core :as knossos]
            [clj-http.client :as httpclient]))

(defn running?
  "Is the service running?"
  []
  (try
    (c/exec :pgrep :-f :ReliableCollectionsWebAPI.dll)
    true
    (catch RuntimeException _ false)))


(defn start!
  "Starts DB."
  [node test]
  (info "Deploying Service to Cluster")
  (info (running?) )
  (info "Deployed Service to Cluster"))




(defn stop!
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


(defn db []

  (reify jdb/DB
    (setup! [_ test node]
      (info "not yet complete")
      (info "git clone github.com/doommius/thesis.git")
      (info "cd thesis/Jepsen")
      (info "deploy Dotnet application to cluster")
      (info "Await until clusters and nodes are ready.")
      (start! test node)
      (Thread/sleep 5000)
      )


    (teardown! [_ test node]
      (stop! test node)
      (unprevision! test node)
      )))


(defn just-deploy
  [opts]
  (info "Deploy new service to cluster and waiting until built and deploy is complete.")
  (db)

  )

