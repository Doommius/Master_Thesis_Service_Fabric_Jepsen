(ns jepsen.sfqueue.queue
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
            [jepsen.sfqueue [client :as client] [db :as db] [generators :as generators]]))


  (defn sfqueue-test
        [name opts]
        (merge tests/noop-test
               {:name      (str "sfqueue " name)
                :os        debian/os
                :db        (db/db "f00dd0704128707f7a5effccd5837d796f2c01e3")
                :client    (client/client)
                :model     (model/unordered-queue)
                :generator (->> ()
                                (gen/delay 1)
                                generators/std-gen)
                :checker   (checker/compose {:queue   checker/total-queue
                                             :latency (checker/latency-graph)})}
               opts))
