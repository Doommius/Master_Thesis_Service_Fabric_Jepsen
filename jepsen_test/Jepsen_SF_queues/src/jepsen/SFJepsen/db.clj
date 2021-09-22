(ns jepsen.sfqueue.db
  (:require [clojure [pprint :refer :all]
             [string :as str]]
            [clojure.java.io :as io]
            [clojure.tools.logging :refer [debug info warn]]
            [jepsen [core :as jepsen]
             [db :as db]
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



(def dir "/opt/disque")
(def data-dir "/var/lib/disque")
(def pidfile "/var/run/disque.pid")
(def binary (str dir "/src/disque-server"))
(def control (str dir "/src/disque"))
(def config-file (str dir "/disque.conf"))
(def log-file (str data-dir "/log"))
(def port 35112)

(defn install!
  "Installs DB on the given node. TODO"
  [node version]
  (info node "Deploying application")
  )


(defn configure!
  "Uploads configuration files to the given node."
  [node test]
  (info node "Skip configure!")
  )


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

(defn join!
  "Cluster does this on it's own.."
  [node test]
  ; Join everyone to primary
  (jepsen/synchronize test)
  (let [p (jepsen/primary test)]
    (when-not (= node p)
      (info node "joining" p)
      (let [res (c/exec control "-p" port
                        :cluster :meet (net/ip (name p)) port)]
        (assert (re-find #"^OK$" res))))))


(defn wipe!
  "Remove and redeploy application."
  [node]
  (info node "deleting data files")
  )

(defn db [version]
  (reify db/DB
    (setup! [_ test node]
      (doto node
        (install! version)
        (configure! test)
        (start! test)
        (join! test)))

    (teardown! [_ test node]
      (wipe! node))

    db/LogFiles
    (log-files [_ _ _] [log-file])))


(defn teardown!
  "Remove and redeploy application."
  [node, test]
  (info node "deleting data files")
  )



(defn queueenqueue [node item client-timeout]
  (httpclient/put str ("http://" (name node) ":35112/API/ReliableDictionary" "/" item) {:socket-timeout client-timeout :connection-timeout client-timeout})
  )

(defn queuecount [node, client-timeout]
  (httpclient/get str ("http://" (name node) ":35112/API/ReliableDictionary") {:socket-timeout client-timeout :connection-timeout client-timeout})
  )
(defn queuepeek [node, client-timeout]
  (httpclient/get str ("http://" (name node) ":35112/API/ReliableDictionary" "/peek") {:socket-timeout client-timeout :connection-timeout client-timeout})
  )

(defn queuedequeue [node, client-timeout]
  (info "dequeue with perameters" node client-timeout)
  (httpclient/delete str ("http://" (name node) ":35112/API/ReliableDictionary") {:socket-timeout client-timeout :connection-timeout client-timeout})
  )

(defrecord Client [node conn]
  client/Client
  (open! [this test node]
    (let [c (c/open node)]
      (assoc this
        :node node
        :conn c
        )))

  (setup! [_ test]
    (info "dequeue all")
    )

  (invoke! [_ test op]
    (case (:f op)
    :dequeue (assoc op :type :ok, :value (queuedequeue node 100 ))
    :enqueue (do (queueenqueue node (:value op) 100  ) (assoc op :type :ok))
    :queuesize (assoc op :type :ok, :value (queuecount node 100 ))
    :queuepeek (assoc op :type :ok, :value (queuepeek node 100 ))))

  (teardown! [_ test])

  (close! [this test]
    (c/close! conn)))

(defn workload
  "A list append workload."
  [opts]
  (-> (append/test (assoc (select-keys opts [:key-count
                                             :max-txn-length
                                             :max-writes-per-key])
                     :min-txn-length 1
                     :consistency-models [(:expected-consistency-model opts)]))
      (assoc :client (Client. nil nil nil))))