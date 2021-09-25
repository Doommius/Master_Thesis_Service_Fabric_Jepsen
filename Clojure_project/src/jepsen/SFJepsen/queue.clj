(ns jepsen.SFJepsen.queue
  (:require [clojure [pprint :refer :all]
             [string :as str]]
            [clojure.tools.logging :refer [debug info warn]]
            [jepsen
             [client :as client]
             [generator :as gen]]
            [jepsen.tests.cycle.append :as append]
            [jepsen.SFJepsen.Driver.core :as sfc]

            )
  (:import (java.util.concurrent TimeUnit)
           (knossos.model Model)
           (java.util UUID)
           (java.io IOException)))


(defn client-url [node]
 (concat "http://"  node ":35112/api")
  )

(defrecord Client [conn]
  client/Client
  (open! [this test node]
    (assoc this :conn (sfc/connect (client-url node)
                                 {:timeout 5000})))

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
    (.shutdown conn)))

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

(defn workload
  "A package of client, checker, etc."
  [opts]
  {:client    (Client.  nil )
   :generator (queue-gen)})
