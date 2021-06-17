(ns jepsen.etcd
  (:require [clojure.tools.logging :refer :all]
            [clojure.string :as str]
            [jepsen [checker :as checker]
             [cli :as cli]
             [client :as client]
             [control :as c]
             [db :as db]
             [generator :as gen]
             [nemesis :as nemesis]
             [tests :as tests]]
            [jepsen.checker.timeline :as timeline]
            [jepsen.control.util :as cu]
            [jepsen.os.debian :as debian]
            [knossos.model :as model]
            [slingshot.slingshot :refer [try+]]
            [verschlimmbesserung.core :as v]))


(def dir     "/opt/etcd")
(def binary "etcd")
(def logfile (str dir "/etcd.log"))
(def pidfile (str dir "/etcd.pid"))
(def dir "/opt/etcd")

(defn r   [_ _] {:type :invoke, :f :read, :value nil})
(defn w   [_ _] {:type :invoke, :f :write, :value (rand-int 5)})
(defn cas [_ _] {:type :invoke, :f :cas, :value [(rand-int 5) (rand-int 5)]})

(defn node-url
  "An HTTP url for connecting to a node on a particular port."
  [node port]
  (str "http://" node ":" port))

(defn peer-url
  "The HTTP url for other peers to talk to a node."
  [node]
  (node-url node 2380))

(defn client-url
  "The HTTP url clients use to talk to a node."
  [node]
  (node-url node 2379))

(defn initial-cluster
  "Constructs an initial cluster string for a test, like
  \"foo=foo:2380,bar=bar:2380,...\""
  [test]
  (->> (:nodes test)
       (map (fn [node]
              (str node "=" (peer-url node))))
       (str/join ",")))

(defn parse-long
  "Parses a string to a Long. Passes through `nil`."
  [s]
  (when s (Long/parseLong s)))

(defn cluster
  "etcd for a particular version."
  [version]
  (reify db/DB
         (setup! [_ test node]
;                 Deploy Test code to cluster
                 (info node "Deploying Config to etcd test " version)
                 (c/su

                  (cu/start-daemon!
                   {:logfile logfile
                    :pidfile pidfile
                    :chdir   dir}
                   binary
                   :--log-output                   :stderr
                   :--name                         (name node)
                   :--listen-peer-urls             (peer-url   node)
                   :--listen-client-urls           (client-url node)
                   :--advertise-client-urls        (client-url node)
                   :--initial-cluster-state        :new
                   :--initial-advertise-peer-urls  (peer-url node)
                   :--initial-cluster              (initial-cluster test))

                  (Thread/sleep 10000)))

         (teardown! [_ test node]
;                    TODO change it to reset cluster.
                    (info node "tearing down etcd")
                    (cu/stop-daemon! binary pidfile)
                    (c/su (c/exec :rm :-rf dir)))


         db/LogFiles
         (log-files [_ test node]
                    [logfile])))

(defrecord Client [conn]
  client/Client
  (open! [this test node]
    (assoc this :conn (v/connect (client-url node)
                                 {:timeout 5000})))

  (setup! [this test])

  (invoke! [this test op]
    (case (:f op)
      :read (let [value (-> conn
                            (v/get "foo" {:quorum? true})
                            parse-long)]
              (assoc op :type :ok, :value value))
      :write (do (v/reset! conn "foo" (:value op))
               (assoc op :type :ok))
      :cas (try+
            (let [[old new] (:value op)]
              (assoc op :type (if (v/cas! conn "foo" old new)
                                :ok
                                :fail)))
            (catch [:errorCode 100] ex
              (assoc op :type :fail, :error :not-found)))))

  (teardown! [this test])

  (close! [_ test]
    ; If our connection were stateful, we'd close it here. Verschlimmmbesserung
    ; doesn't actually hold connections, so there's nothing to close.
    ))

(defn etcd-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh,
  :concurrency ...), constructs a test map."
  [opts]
  (merge tests/noop-test
         opts
         {:pure-generators true
          :name            "etcd"
          :os              debian/os
          :db              (cluster "v3.1.5")
          :client          (Client. nil)
          :nemesis         (nemesis/partition-random-halves)
          :checker         (checker/compose
                            {:perf   (checker/perf)
                             :linear (checker/linearizable
                                      {:model     (model/cas-register)
                                       :algorithm :linear})
                             :timeline (timeline/html)})
          :generator (->> (gen/mix [r w cas])
                          (gen/stagger 1/50)
                          (gen/nemesis
                           (cycle [(gen/sleep 5)
                                   {:type :info, :f :start}
                                   (gen/sleep 5)
                                   {:type :info, :f :stop}]))
                          (gen/time-limit (:time-limit opts)))}))


(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn etcd-test})
                   (cli/serve-cmd))
            args))



