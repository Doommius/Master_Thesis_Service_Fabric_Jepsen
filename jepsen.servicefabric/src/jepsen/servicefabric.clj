(ns jepsen.servicefabric
  (:require [clojure.tools.logging :refer :all]
            [clojure.string :as str]
            [jepsen [cli :as cli]
                    [control :as c]
                    [db :as db]
                    [tests :as tests]]
            [jepsen.control.util :as cu]
            [jepsen.os.debian :as debian]))

(defn servicefabric-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh,
  :concurrency, ...), constructs a test map."
  [opts]
  (merge tests/noop-test
         opts
         {:name "service fabric"
          :os   debian/os
          :db   (db "v8.0.513.1804")
          :pure-generators true}))
(defn db
  "servicefabric DB for a particular version."
  [version]
  (reify db/DB
    (setup! [_ test node]
      (info node "installing servicefabric" version))
    (teardown! [_ test node]
      (info node "tearing down servicefabric"))))
(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn servicefabric-test})
                   (cli/serve-cmd))
            args))