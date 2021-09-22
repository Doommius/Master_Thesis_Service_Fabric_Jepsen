(ns jepsen.sfqueue.generators
  (:require [clojure [pprint :refer :all]
             [string :as str]]
            [clojure.java.io :as io]
            [clojure.tools.logging :refer [debug info warn]]
            [jepsen [core :as jepsen]
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
            [clj-http.client :as httpclient]
            [jepsen.sfqueue [db :as db]]

            )
  )



  (defn killer
        "Kills a random node on start, restarts it on stop."
        []
        (nemesis/node-start-stopper
          rand-nth
          (fn start [test node] (db/stop! node test))
          (fn stop [test node] (db/start! node test))))

  ; Generators

  (defn std-gen
        "Takes a client generator and wraps it in a typical schedule and nemesis
        causing failover."
        [gen]
        (gen/phases
          (->> gen
               (gen/nemesis
                 ((cycle [(gen/sleep 10)
                                  {:type :info :f :start}
                                  (gen/sleep 10)
                                  {:type :info :f :stop}])))
               (gen/time-limit 100))
          ; Recover
          (gen/nemesis (gen/once {:type :info :f :stop}))
          ; Wait for resumption of normal ops
          (gen/clients (gen/time-limit 10 gen))
          ; Drain
          (gen/log "Draining")
          (gen/clients ( (gen/once {:type :invoke
                                            :f    :drain})))))

