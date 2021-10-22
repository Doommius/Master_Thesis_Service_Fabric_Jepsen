(ns jepsen.SFJepsen.dict
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
            [knossos.model :as model]
            [clojure.set :as set]
            [jepsen.tests.cycle.append :as append]
            [jepsen.checker.timeline :as timeline]
            [jepsen.SFJepsen.Driver.core :as sfc]


            [jepsen.nemesis :as nemesis]
            )
  )


(defn r [_ _] {:type :invoke, :f :read, :value nil})
(defn d [_ _] {:type :invoke, :f :delete, :value nil})
(defn w [_ _] {:type :invoke, :f :write, :value (rand-int 5)})
(defn cas [_ _] {:type :invoke, :f :cas, :value [(rand-int 5) (rand-int 5)]})




(defn client-url [node]
  (node)
  )

(defrecord  Client [conn]
  client/Client
    (open! [this test node]
      (assoc this :conn (sfc/connect  node)))

    (setup! [this test])

    (invoke! [_ test op]
      (let [[k v] (:value op)]
        (try+
          (case (:f op)
            :read (let [value (sfc/get conn k {:quorum? true})]
                    (info value)
                    (assoc op :type :ok, :value (independent/tuple k value)))
            ;:read (do (sfc/get conn (:value op))
            ;          (assoc op :type :ok))
            :delete (do (sfc/delete conn k)
                        (assoc op :type :ok))
            :write (do (sfc/write conn k v)
                       (assoc op :type :ok))
            :txn (do (sfc/write conn k v)
                       (assoc op :type :ok))
            :insert (do (sfc/write conn k v)
                       (assoc op :type :ok))
            :cas (let [[old new] v]
                   (assoc op :type (if (= new (sfc/cas conn k new old))
                                     :ok
                                     :fail))))
          (catch java.net.SocketTimeoutException e
            (assoc op
              :type (if (= :read (:f op)) :fail :info)
              :error :timeout))
          (catch java.net.ConnectException e
            ;(info e)
            (assoc op
              :type (if (= :read (:f op)) :fail :info)
              :error :ConnectException))
          (catch [:stutus 400] e
            (assoc op :type :fail, :error :Connectrefused))
          (catch [:stutus 204] e
            (assoc op :type :fail, :error :not-found))
          )
        ))

    (teardown! [_ test]
      )

    (close! [_ test]
      ; If our connection were stateful, we'd close it here.
      ; we doesn't actually hold connections, so there's nothing to close.
      ))


(defn dict-gen
  "A generator for queue operations. Emits enqueues of sequential integers."
  [opts]
  (->> (independent/concurrent-generator
         20
         (range)
         (fn [k]
           (->> (gen/mix [r w cas])
                (gen/limit (:ops-per-key opts))))
         )
       (gen/nemesis
         (cycle [(gen/sleep 5)
                 {:type :info, :f :start}
                 (gen/sleep 5)
                 {:type :info, :f :stop}]))
       (gen/time-limit (:time-limit opts))
       ))

(defn gen
  "Wrapper for elle.list-append/gen; as a Jepsen generator."
  [opts]
  (la/gen opts))

(defn checker
  "Full checker for append and read histories. See elle.list-append for
  options."
  ([]
   (checker {:anomalies [:G1 :G2]}))
  ([opts]
   (reify checker/Checker
     (check [this test history checker-opts]
       (la/check (assoc opts :directory
                             (.getCanonicalPath
                               (store/path! test (:subdirectory checker-opts) "elle")))
                 history)))))
(defn workload
  "A package of client, checker, etc."
  [opts]
  {:client    (Client. nil)
   :checker   (checker/compose
                { :perf       (checker/perf)
                 :clock      (checker/clock-plot)
                 :stats      (checker/stats)
                 :exceptions (checker/unhandled-exceptions)
                 })

   :generator (dict-gen opts)

   ;:checker   (checker opts)
   ;:generator  (la/gen opts)

   })
