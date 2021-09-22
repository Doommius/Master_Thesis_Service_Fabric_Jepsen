(ns jepsen.sfqueue.bank
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
             [store :as store]
             [report :as report]
             [tests :as tests]]
            [jepsen.os.debian :as debian]
            [knossos.core :as knossos]
            [clj-http.client :as httpclient]
            [jepsen.sfqueue[queue :as queue] [nemesis :as nemesis]]

            ))


(defn single-node-restarts-test
  []
  (queue/sfqueue-test "single node restarts"
                {:nemesis (nemesis/kill-node)}))

(defn partitions-test
  []
  (queue/sfqueue-test "partitions"
                {:nemesis (nemesis/partition-random-halves)}))


(defn alltest
  (partitions-test)
  (single-node-restarts-test)
  )
