(ns jepsen.sfqueue.client
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
             [client :as jcclient]
             [checker :as checker]
             [generator :as gen]
             [nemesis :as nemesis]
             [store :as store]
             [report :as report]
             [tests :as tests]]
            [jepsen.control [net :as net]
             [util :as cu]]
            [jepsen.os.debian :as debian]
            [knossos.core :as knossos]
            [clj-http [client :as httpclient]]
            [jepsen.client :as client])
)

(defn queueenqueue [node item, client-timeout]
  (httpclient/put str ("http://" (name node) ":35112/API/ReliableDictionary" "/" item) {:socket-timeout client-timeout :connection-timeout client-timeout})
  )

(defn queuecount [node, client-timeout]
  (httpclient/get str ("http://" (name node) ":35112/API/ReliableDictionary") {:socket-timeout client-timeout :connection-timeout client-timeout})
  )
(defn queuepeek [node, client-timeout]
  (httpclient/get str ("http://" (name node) ":35112/API/ReliableDictionary" "/peek") {:socket-timeout client-timeout :connection-timeout client-timeout})
  )

(defn queuedequeue [node, client-timeout op]
  (info "dequeue with perameters" node client-timeout)
  (httpclient/delete str ("http://" (name node) ":35112/API/ReliableDictionary") {:socket-timeout client-timeout :connection-timeout client-timeout})
  )

":retry-handler (fn [ex try-count http-context
                (println \"Got:\" ex)
                (if (> try-count 4) false true))"


(defrecord Client [queue client-timeout]
  client/Client

  (open! [this test node]
    this)

  (setup! [this test])


  (invoke! [this test op]
    (case (:f op)
      :enqueue (
                (queueenqueue node (str (:value op)) client-timeout)
                (assoc op :type :ok))
      :dequeue (queuedequeue node client-timeout op)
      :peek (queuepeek node client-timeout)
      :count (queuecount node client-timeout)
      :drain (timeout 10000 (assoc op :type :info :value :timeout)
                      (loop []
                        (let [op' (->> (assoc op
                                         :f :dequeue
                                         :time (relative-time-nanos))
                                       util/log-op
                                       (jepsen/conj-op! test)
                                       (queuedequeue node client-timeout))]
                          ; Log completion
                          (->> (assoc op' :time (relative-time-nanos))
                               util/log-op
                               (jepsen/conj-op! test))

                          (if (= :fail (:type op'))
                            ; Done
                            (assoc op :type :ok, :value :exhausted)

                            ; Keep going.
                            (recur))))))

    (teardown! [this test]
               (.close client)))

  )
(defn client
  []
  (Client. "jepsen"
           100
           (-> (JobParams.)
               (.setRetry (int 1))
               (.setReplicate (int 3)))))
