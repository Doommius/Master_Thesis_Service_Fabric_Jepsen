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
                                 :qe (sfc/parseresult r)
                                 :qd (sfc/parseresult r)
                                 :qp (sfc/parseresult r)
                                 :qc (sfc/parseresult r)
                                 :qa (sfc/parseresult r)
                                 )])
                        (:value op))
                  (assoc op :type :ok, :value)
                  )
        )

      (catch [:status 500] e
        (assoc op :type :fail, :error :internal-server-error))
      (catch [:status 400] e
        (assoc op :type :fail, :error :bad-request))
      (catch [:status 204] e
        (assoc op :type :fail, :error :not-found))
      (catch [:status 601] e
        (assoc op :type :fail, :error :RealiableCollectionslockTimeout))
      (catch [:status 602] e
        (assoc op :type :fail, :error :notprimary))
      (catch java.net.SocketTimeoutException e
        (assoc op
          :type (if (= :read (:f op)) :fail :info)
          :error :timeout))
      (catch java.net.ConnectException e
        ;(info e)
        (assoc op
          :type (if (= :read (:f op)) :fail :info)
          :error :ConnectException))))

  (teardown! [this test])


  (close! [_ test]
    ; If our connection were stateful, we'd close it here.
    ; we doesn't actually hold connections, so there's nothing to close.
    )

  )




(defn qe   [_ _] {:type :invoke, :f :txn , :value (rand-int 5)})
(defn qd   [_ _] {:type :invoke, :f :txn , :value nil})


(defn workload
  "A package of client, checker, etc."
  [opts]
  {:client    (Client. nil)
   :model     (model/unordered-queue)
   :generator (->> (gen/mix [qe, qd]))
   :checker   (checker/compose {:queue   checker/total-queue
                                :latency (checker/latency-graph)})})
