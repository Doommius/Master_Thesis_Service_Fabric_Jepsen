(ns jepsen.SFJepsen.queue
  (:require [clojure [pprint :refer :all]
             [string :as str]]
            [clojure.tools.logging :refer [debug info warn]]
            [jepsen
             [client :as client]
             [checker :as checker]
             [generator :as gen]]
            [elle.graph :as elle]
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
        :txn (->> (sfc/txn conn sfc/RQuri op)
                  (mapv (fn [[f k v] r]
                          [f k (case f
                                 :e (sfc/parseresult r)
                                 :d (sfc/parseresult r)
                                 :p (sfc/parseresult r)
                                 :c (sfc/parseresult r)
                                 :a (sfc/parseresult r)
                                 :append v)])
                        (:value op))
                  (assoc op :type :ok, :value)
                  )
        )
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
      (catch [:status 405] e
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

(defn workload
  "A package of client, checker, etc."
  [opts]
  {:client    (Client. nil)
   :model     (model/unordered-queue)
   :generator (->> (elle.list-append/gen [opts]))
   :checker   (checker/compose {:queue   checker/total-queue
                                :latency (checker/latency-graph)})})
