(ns jepsen.sf
  (:gen-class)
  (:require [clojure.tools.logging :refer [debug info warn]]
            [clojure.string :as str]
            [jepsen.cli :as cli]
            [jepsen.os.debian :as debian]
            [jepsen.checker :as checker]
            [jepsen.generator :as gen]
            [jepsen.nemesis :as nemesis]
            [jepsen.tests :as tests]
            [jepsen.sf.register :as register]
            [jepsen.sf.client :as cc]
            [jepsen.sf.db :as db]))

;; Default is nil
(def consistency-levels
  #{"stale" "consistent"})

(def workloads
  {"none"     (fn [_] tests/noop-test)
   "register" register/workload})

(defn sf-test
  "Given an options map from the command line runner constructs a test map. Special options:"
  [opts]
  (let [workload-name (:workload opts)
        workload ((workloads workload-name) opts)
        db (db/db)
        nemesis (nemesis/partition-random-halves)]
    (merge tests/noop-test
           opts
           {:name (str "sf " (:version opts) " " workload-name)
            :os debian/os
            :db db
            :initialized? (atom false)
            :nemesis nemesis
            :checker (checker/compose
                      {:perf        (checker/perf {:nemeses (:perf nemesis)})
                       :clock       (checker/clock-plot)
                       :stats       (checker/stats)
                       :exceptions  (checker/unhandled-exceptions)
                       :workload    (:checker workload)})
            :client    (:client workload)
            :generator (gen/phases
                        (->> (:generator workload)
                             (gen/stagger (/ (:rate opts)))
                             (gen/nemesis
                              (gen/seq
                               (cycle [(gen/sleep 10)
                                       {:type :info :f :start}
                                       (gen/sleep 10)
                                       {:type :info :f :stop}])))
                             (gen/time-limit (or (:time-limit opts) 30)))
                        (gen/log "Healing cluster")
                        (gen/nemesis
                         (gen/once {:type :info :f :stop}))
                        (gen/log "Waiting for recovery")
                        (gen/sleep 10)
                        (gen/clients (:final-generator workload)))})))

(def cli-opts
  "Additional command line options."
  [["-v" "--version STRING" "What version of etcd should we install?"
    :default "1.6.1"]
   ["-w" "--workload NAME" "What workload should we run?"
    :missing  (str "--workload " (cli/one-of workloads))
    :validate [workloads (cli/one-of workloads)]]
   [nil "--consistency LEVEL" "What consistency level to set on kv store requests. Leave empty for default"
    :default nil
    :validate [consistency-levels (cli/one-of consistency-levels)]]
   ["-r" "--rate HZ" "Approximate number of requests per second, per thread."
    :default  10
    :parse-fn read-string
    :validate [#(and (number? %) (pos? %)) "Must be a positive number"]]
   [nil "--ops-per-key NUM" "Maximum number of operations on any given key."
    :default  200
    :parse-fn read-string
    :validate [pos? "Must be a positive integer."]]])

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd
                    {:test-fn sf-test
                     :opt-spec cli-opts})
                   (cli/test-all-cmd {:tests-fn (partial sf-test)
                                      :opt-spec cli-opts})
                   (cli/serve-cmd))
            args))

