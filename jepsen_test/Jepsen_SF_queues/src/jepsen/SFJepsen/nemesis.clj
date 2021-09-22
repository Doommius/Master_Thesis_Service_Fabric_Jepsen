(ns jepsen.sfqueue.nemesis

  (:refer-clojure :exclude [test])
  (:require [clojure.tools.logging :refer :all]
            [jepsen  [nemesis :as nemesis]]
            [jepsen.sfqueue
             [register :as register]
             [bank :as bank]
             [db :as db]]))




(def kill-node
  "Kills random node"
  (nemesis/node-start-stopper
    rand-nth
    (fn start [test node] (db/stop! node test))
    (fn stop [test node] (db/start! node test))))

(def partition-random-halves
  (nemesis/partition-random-halves))