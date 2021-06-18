(ns jepsen.servicefabric
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


(def dir     "/opt/microsoft/servicefabric/bin")
(def binary "/Fabric/Fabric.Code/Fabric")
(def logfile (str dir "/etcd.log"))
(def pidfile (str dir "/etcd.pid"))
(def dir "/opt/etcd")

(defn r   [_ _] {:type :invoke, :f :read, :value nil})
(defn w   [_ _] {:type :invoke, :f :write, :value (rand-int 5)})
(defn cas [_ _] {:type :invoke, :f :cas, :value [(rand-int 5) (rand-int 5)]})



(defn client-url
  "The HTTP url clients use to talk to a node."
  [node]
  (node-url node 80))

(defn parse-long
  "Parses a string to a Long. Passes through `nil`."
  [s]
  (when s (Long/parseLong s)))

(defn cluster
  "service fabric for a particular version."
  [version]
  (reify db/DB
         (setup! [_ test node]
;                 Deploy Test code to cluster, This should setup the service fabric cluster, configure it, deploy api.
                 (info node "Deploying Config to servicefabric test cluster " version)
;                 sudo sh -c 'echo "deb [arch=amd64] https://apt-mo.trafficmanager.net/repos/dotnet-release/ xenial main" > /etc/apt/sources.list.d/dotnetdev.list'
;                 sudo apt-key adv --keyserver apt-mo.trafficmanager.net --recv-keys 417A0893
;                 sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 417A0893
;                 sudo apt-get install curl
;                 sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
;                 sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
;                 sudo apt-get install apt-transport-https
;                 sudo apt-get update
;
;                 echo "servicefabric servicefabric/accepted-eula-ga select true" | sudo debconf-set-selections
;                 echo "servicefabricsdkcommon servicefabricsdkcommon/accepted-eula-ga select true" | sudo debconf-set-selections
;
;                 sudo apt-get install -fy && sudo dpkg -i out/build.prod/FabricDrop/deb/servicefabric_6*.deb
;                 sudo apt-get install -fy && sudo dpkg -i out/build.prod/FabricDrop/deb/servicefabric_sdkcommon_*.deb
;
;                 sudo /opt/microsoft/sdk/servicefabric/common/clustersetup/devclustersetup.sh
                 )

         (teardown! [_ test node]
;                    This should stop the cluster, and clear all files.
                    (info node "Clearing deployed application from servicefabric cluster.")
)

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

(defn servicefabric-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh,
  :concurrency ...), constructs a test map."
  [opts]
  (merge tests/noop-test
         opts
         {:pure-generators true
          :name            "servicefabric Reliable Collections"
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
  (cli/run! (merge (cli/single-test-cmd {:test-fn servicefabric-test})
                   (cli/serve-cmd))
            args))



