(ns jepsen.SFJepsen.concurrentqueue
  (:require [clojure [pprint :refer :all]
             [string :as str]]
            [clojure.tools.logging :refer [debug info warn]]
            [jepsen
             [client :as client]
             [checker :as checker]
             [generator :as gen]]
            [jepsen.tests.cycle.append :as append]
            [slingshot.slingshot :refer [try+]]
            [knossos.model :as model]
            [jepsen.SFJepsen.Driver.core :as sfc]

            )
  (:import (java.util.concurrent TimeUnit)
           (knossos.model Model)
           (java.util UUID)
           (java.io IOException)))


(defn client-url [node]
  (concat "http://" node ":35112/api")
  )

(defrecord Client [conn]
  client/Client
  (open! [this test node]
    (assoc this :conn (sfc/connect node)))

  (setup! [this test])

  (invoke! [this test op]
    (try+
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
                   (assoc op :type :ok, :value values))))
      (catch java.net.SocketTimeoutException e
        (assoc op
          :type (if (= :read (:f op)) :fail :info)
          :error :timeout))
      (catch java.net.ConnectException e
        ;(info e)
        (assoc op
          :type (if (= :read (:f op)) :fail :info)
          :error :ConnectException))
      (catch [:errorCode 400] e
        (assoc op :type :fail, :error :Connectrefused))
      (catch [:status 500] e
        (assoc op :type :fail, :error :internal-server-error))
      (catch [:status 500] e
        (assoc op :type :fail, :error :internal-server-error))
      (catch [:errorCode 204] e
        (assoc op :type :fail, :error :not-found))))

  (teardown! [this test])


  (close! [_ test]
    ; If our connection were stateful, we'd close it here.
    ; we doesn't actually hold connections, so there's nothing to close.
    )

  )


(defn e [_ _] {:type :invoke, :f :enqueue, :value (rand-int 5)})
(defn d [_ _] {:type :invoke, :f :dequeue, :value nil})
(defn c [_ _] {:type :invoke, :f :count, :value nil})



(defn workload
  "A package of client, checker, etc."
  [opts]
  {:client    (Client. nil)
   :model     (model/unordered-queue)
   :generator (->> (gen/mix [e d c]))
   :checker   (checker/compose {:queue   checker/total-queue
                                :latency (checker/latency-graph)})})
