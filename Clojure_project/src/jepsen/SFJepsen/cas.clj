(ns jepsen.SFJepsen.cas
  (:require [clojure             [string :as str]]
            [clojure.tools.logging :refer [debug info warn]]
            [slingshot.slingshot :refer [try+]]
            [jepsen [generator :as gen]
             [client :as client]
             [checker :as checker]
             [independent :as independent]
             ]
            [knossos.model :as model]
            [jepsen.tests.cycle.append :as append]
            [jepsen.checker.timeline :as timeline]
            [jepsen.SFJepsen.Driver.core :as sfc]
            [jepsen.nemesis :as nemesis])
  )






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
                  (assoc op :type :ok, :value (independent/tuple k value)))
          :delete (do (sfc/delete conn k)
                      (assoc op :type :ok))
          :write (do (sfc/write conn k v)
                     (assoc op :type :ok))
          :cas (let [[old new] v]
                 (assoc op :type (if (= new (sfc/cas conn k old new))
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
        (catch [:errorCode 400] e
          (assoc op :type :fail, :error :Connectrefused))
        (catch [:errorCode 204] e
          (assoc op :type :fail, :error :not-found))
        )
      ))

  (teardown! [_ test]
    )

  (close! [_ test]
    ; If our connection were stateful, we'd close it here.
    ; we doesn't actually hold connections, so there's nothing to close.
    ))




(defn r [_ _] {:type :invoke, :f :read, :value nil})
(defn d [_ _] {:type :invoke, :f :delete, :value nil})
(defn w [_ _] {:type :invoke, :f :write, :value (rand-int 5)})
(defn cas [_ _] {:type :invoke, :f :cas, :value [(rand-int 5) (rand-int 5)]})

(defn dict-gen
  "A generator for queue operations. Emits enqueues of sequential integers."
  [opts]
  (->> (independent/concurrent-generator
         20
         (range)
         (fn [k]
           (->> (gen/mix [r w cas])
                (gen/limit (:ops-per-key opts)))))
       ))



(defn workload
  "A package of client, checker, etc."
  [opts]
  {:client    (Client. nil)
   :checker   (checker/compose
                {:perf  (checker/perf)
                 :indep (independent/checker
                          (checker/compose
                            {:linear   (checker/linearizable {:model     (model/cas-register)
                                                              :algorithm :linear})
                             :timeline (timeline/html)}))})
   :generator (dict-gen opts)})
