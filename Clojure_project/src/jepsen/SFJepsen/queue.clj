(ns jepsen.SFJepsen.queue
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
            [jepsen.tests.cycle.append :as append]
            [knossos.core :as knossos]
            [jepsen.SFJepsen.Driver.core :as sfc]
            )
  (:import (java.util.concurrent TimeUnit)
           (knossos.model Model)
           (java.util UUID)
           (java.io IOException)))


(def queue-poll-timeout
  "How long to wait for items to become available in the queue, in ms"
  1)

(defrecord Client [conn]
  "Uses :enqueue, :dequeue, and :drain events to interact with a Hazelcast queue."
  (reify client/Client
    (setup! [_ test node]
      (let [conn (sfc/connect (concat "http://" node ":35112/api"))]
        (Client conn)))

    (invoke! [this test op]
      (case (:f op)
        :enqueue (do (sfc/enqueue conn (:value op))
                     (assoc op :type :ok))
        :dequeue (if-let [v (sfc/dequeue conn)]
                   (assoc op :type :ok, :value v)
                   (assoc op :type :fail, :error :empty))
        :peek (if-let [v (sfc/queuepeek conn)]
                (assoc op :type :ok, :value v)
                (assoc op :type :fail, :error :empty))
        :count (if-let [v (sfc/queuecount conn)]
                 (assoc op :type :ok, :value v)
                 (assoc op :type :fail, :error :empty))
        :drain (loop [values []]
                 (if-let [v (repeat (sfc/queuecount conn) (sfc/dequeue conn))]
                   (recur (conj values v))
                   (assoc op :type :ok, :value values)))))

    (teardown! [this test]
      (.shutdown conn))))

(defn queue-gen
  "A generator for queue operations. Emits enqueues of sequential integers."
  []
  (let [next-element (atom -1)]
    (->> (gen/mix [(fn enqueue-gen [_ _]
                     {:type  :invoke
                      :f     :enqueue
                      :value (swap! next-element inc)})
                   {:type :invoke, :f :dequeue}])
         (gen/stagger 1))))

(defn queue-client-and-gens
  "Constructs a queue client and generator. Returns {:client
  ..., :generator ...}."
  []
  {:client          (Client)
   :generator       (queue-gen)
   :final-generator (->> {:type :invoke, :f :drain}
                         gen/once
                         gen/each)})

(defn workload
  "A list append workload."
  [opts]
  (-> (append/test (assoc (select-keys opts [:key-count
                                             :max-txn-length
                                             :max-writes-per-key])
                     :min-txn-length 1
                     :consistency-models [(:expected-consistency-model opts)]))
      (assoc :client (new queue-client-and-gens nil))))