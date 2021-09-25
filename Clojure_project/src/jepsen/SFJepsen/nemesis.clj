(ns jepsen.SFJepsen.nemesis
  "Nemeses for SF"
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :refer [info warn]]
            [dom-top.core :refer [real-pmap]]
            [jepsen [nemesis :as n]
             [net :as net]
             [util :as util]]
            [jepsen.generator :as gen]
            [jepsen.nemesis [combined :as nc]
             [time :as nt]]
            [jepsen.SFJepsen.db :as db]
            [jepsen.nemesis :as nemesis]))


(defn nemesis-package
  "Constructs a nemesis and generators for SF."
  [opts]
  (let [opts (update opts :faults set)]
    (nc/nemesis-package opts)))


(defn killer
  "Kills a random node on start, restarts it on stop."
  []
  (nemesis/node-start-stopper
    rand-nth
    (fn start [test node] (db/stop! node test))
    (fn stop [test node] (db/start! node test))))
