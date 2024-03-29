(ns jepsen.SFJepsen.dict
  (:require [clojure [string :as str]]
            [clojure.tools.logging :refer [debug info warn]]
            [slingshot.slingshot :refer [try+]]
            [elle.core :as elle]
            [elle.rw-register :as ellerw]

            [jepsen [generator :as gen]
             [client :as client]
             [checker :as checker]
             [independent :as independent]
             [store :as store]
             ]
            [knossos.model :as model]
            [clojure.set :as set]
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
          :txn (->> (sfc/txn conn sfc/RDuri k)
                    (mapv (fn [[f k v] r]
                            [f k (case f
                                   :r (sfc/parseresult r)
                                   :w (sfc/parseresult r)
                                   :cas (sfc/parseresult r)
                                   :d (sfc/parseresult r)
                                   :a (sfc/parseresult r)
                                   :append v)])
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
          (assoc op :type :fail, :error e))
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

(defn checker
  "Full checker for append and read histories. See elle.list-append for
  options."
[opts]
   (reify checker/Checker
     (check [this test history checker-opts]
       (ellerw/check (assoc opts :directory
                                 (.getCanonicalPath
                                   (store/path! test (:subdirectory checker-opts) "elle")))
                     history))))
(defn workload
  "A package of client, checker, etc."
  [opts]
  {
   :generator (ellerw/gen opts)
   :client    (Client. nil)
   :checker   (checker/compose
                {:perf       (checker/perf)
                 :clock      (checker/clock-plot)
                 :stats      (checker/stats)
                 :exceptions (checker/unhandled-exceptions)
                 :elle       (checker (assoc opts :anomalies [:G0 :G1 :G2 :GSIa :GSIb]))
                 }
                )



   ;:generator  (la/gen opts)

   })
