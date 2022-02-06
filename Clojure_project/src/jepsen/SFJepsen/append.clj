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
            [jepsen.checker.timeline :as timeline]
            [jepsen.tests.long-fork :as longfork]
            [jepsen.SFJepsen.Driver.core :as sfc]


            [jepsen.nemesis :as nemesis]
            )
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
         (assoc op :type :fail, :error :deadlock, :description (:description e)))
      (catch [:status 602] e
        (assoc op :type :fail, :error :notprimary))
      (catch Exception e
        (assoc op :type :fail, :error e)
        )
      ;(finally
        ;(info "OP is " op)
        ;(info "K is " k))
      )
      )

    )

  (teardown! [_ test]
    )

  (close! [_ test]
    ; If our connection were stateful, we'd close it here.
    ; we doesn't actually hold connections, so there's nothing to close.
    ))


(defn workload
  "A package of client, checker, etc."
  [opts]
  {
   :generator (append/gen opts)
   :client    (Client. nil)
   :checker   (append/checker (assoc opts :anomalies [:G0 :G2  :incompatible-order :dirty-update]))
   }

  )
