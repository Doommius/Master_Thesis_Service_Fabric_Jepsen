(ns jepsen.SFJepsen.bank
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
             [client :as client]
             [checker :as checker]
             [generator :as gen]
             [store :as store]
             [report :as report]
             [tests :as tests]]
            [jepsen.os.debian :as debian]
            [knossos.core :as knossos]
            [clj-http.client :as httpclient]
            [jepsen.SFJepsen [queue :as queue]]
            ))


(defn reliablequeue
  [opts]
  (print "awaiting (queue/queuetest opts)" )
  )
(defn alltest
  [opts]
  (print "awaiting   (reliablequeue opts)" )

  )
