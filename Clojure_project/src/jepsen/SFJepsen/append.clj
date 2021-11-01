(ns jepsen.SFJepsen.append
  (:require [clojure [string :as str]]
            [clojure.tools.logging :refer [debug info warn]]
            [slingshot.slingshot :refer [try+]]
            [elle.core :as elle]
            [elle.list-append :as la]
            [jepsen [generator :as gen]
             [client :as client]
             [checker :as checker]
             [independent :as independent]
             [store :as store]
             ]
            [jepsen.checker.timeline :as timeline]
            [jepsen.tests.cycle.append :as append]
            [jepsen.tests.cycle.append :as append]
            [jepsen.checker.timeline :as timeline]
            [jepsen.SFJepsen.Driver.core :as sfc]


            [jepsen.nemesis :as nemesis]
            )
  )

(defn client-url [node]
  (node)
  )




(defrecord Client [conn]
  client/Client
  (open! [this test node]
    (assoc this :conn (sfc/connect node)))

  (setup! [this test])

  (invoke! [_ test op]
    (let [k (:value op)]
      (try+
        ;(warn op)
        (case (:f op)
          :txn (->> (sfc/txn conn sfc/RAuri k)
                    (mapv (fn [[f k v] r]
                            [f k (case f
                                   :r (sfc/parseresultlist r)
                                   :append v, (sfc/parseresultlist r))])
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
          (assoc op :type :fail, :error :timeout))
        (catch java.net.ConnectException e
          (assoc op :type :fail, :error :ConnectException))
        )
      ))

  (teardown! [_ test]
    )

  (close! [_ test]
    ; If our connection were stateful, we'd close it here.
    ; we doesn't actually hold connections, so there's nothing to close.
    ))

(defn workload
  "A package of client, checker, etc."
  [opts]

  (-> (append/test {; Exponentially distributed, so half of the time it's gonna
                    ; be one key, 3/4 of ops will use one of 2 keys, 7/8 one of
                    ; 3 keys, etc.
                    :key-count          (:key-count opts 12)
                    :min-txn-length     1
                    :max-txn-length     (:max-txn-length opts 4)
                    :max-writes-per-key (:max-writes-per-key opts 128)
                    :consistency-models [:repeatable-read]
                    })
      (assoc :client (Client. nil))
      (assoc :generator (la/gen opts))

      ))
