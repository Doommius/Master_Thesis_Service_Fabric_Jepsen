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
            [jepsen.control [net :as net]
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
  (info node "Deploying Server")
  (c/su
    (assert (not (running?)))
    (info node "started")))


(defn stop!
  "Stops DB."
  [node test]
  (info node "kill instance of DB")
  (c/su
    (meh (c/exec :sudo :killall :-9 :-u :sfappsuser)))
  )


(defn db  [opts]
  (println opts)
  (info "not yet complete")
  (info "git clone github.com/doommius/thesis.git")
  (info "cd thesis/Jepsen")
  (info "deploy Dotnet application to cluster")
  (info "Await until clusters and nodes are ready.")


  )

(defn just-deploy
  [opts]
  (info "Deploy new service to cluster and waiting until built and deploy is complete.")
  (db opts)

  )

