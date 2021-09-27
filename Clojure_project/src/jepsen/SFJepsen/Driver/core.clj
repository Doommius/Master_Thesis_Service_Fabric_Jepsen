(ns jepsen.SFJepsen.Driver.core
  "Core Raft API operations over HTTP. Clients are currently stateless, but you
  may maintain connection pools going forward. In general, one creates a client
  using (connect) and uses that client as the first argument to all API
  functions.

  Every operation may take a map of options as an optional final argument.
  These options are remapped from :clojure-style keys to their Raft equivalents
  and passed as the query parameters of the request; with the exception of a
  few keys like :timeout; see http-opts for details.

  Functions with a bang, like reset!, mutate state. All other functions are
  pure.

  Some functions come in pairs, like get and get*.

  The get* variant returns the full etcd response body as a map, as specified
  by http://coreos.com/docs/distributed-configuration/etcd-api/. Note that
  values are strings; verschlimmbesserung does not provide value
  serialization/deserialization yet.

  The get variant returns a more streamlined representation: just the node
  value itself."
  (:refer-clojure :exclude [swap! reset! get set])
  (:require [clojure.core.reducers :as r]
            [clojure.string :as str]
            [clojure.tools.logging :refer [debug info warn]]
            [clj-http.client :as http]
            [clj-http.util :as http.util]
            [cheshire.core :as json]
            [slingshot.slingshot :refer [try+ throw+]])
  (:import (com.fasterxml.jackson.core JsonParseException)
           (java.io InputStream)
           (clojure.lang MapEntry)))

(def api-version "")
(def RDuri "ReliableDictionary")
(def Quri "ReliableConcurrentQueue")
(def CQuri "ReliableConcurrent")

(def default-timeout "milliseconds" 200)

(def default-swap-retry-delay
  "How long to wait (approximately) between retrying swap! operations which
  failed. In milliseconds."
  100)

(defn connect
  "Creates a new etcd client for the given server URI. Example:

  (def etcd (connect \"http://127.0.0.1:4001\"))

  Options:

  :timeout            How long, in milliseconds, to wait for requests.
  :swap-retry-delay   Roughly how long to wait between CAS retries in swap!"
  ([server-uri]
   (connect server-uri {}))
  ([server-uri opts]
   (merge {:timeout          default-timeout
           :swap-retry-delay default-swap-retry-delay
           :endpoint         server-uri}
          opts)))

(defn base-url
  "Constructs the base URL for all etcd requests. Example:

  (base-url client) ; => \"http://127.0.0.1:4001/v2\""
  [clientaddress]
  (str "http://" (:endpoint clientaddress) ":35112/api"))


(defn ^String url
  "The URL for a key under a specified root-key.
  (url client [\"keys\" \"foo\"]) ; => \"http://127.0.0.1:4001/v2/keys/foo"
  ([client uri]

   (str (base-url client) "/" uri))
  ([client uri key-seq]

   (str (base-url client) "/" uri "/" key-seq))
  ([client uri key value]

   (str (base-url client) "/" uri "/" key "/" value))
  ([client uri key value1 value2]

   (str (base-url client) "/" uri "/" key "/" value1 "/" value2)))



(defn http-opts
  "Given a map of options for a request, constructs a clj-http options map.
  :timeout is used for the socket and connection timeout. Remaining options are
  passed as query params."
  [client opts]
  {:as                    :string
   :throw-exceptions?     true
   :throw-entire-message? true
   :follow-redirects      true
   :force-redirects       true                              ; Etcd uses 307 for side effects like PUT
   :socket-timeout        (or (:timeout opts) (:timeout client))
   :conn-timeout          (or (:timeout opts) (:timeout client))})

(defn parse
  "Parse an inputstream or string as JSON"
  [response]

  (debug response)
  (when (= 202 (response :status)) true)

  (when (= 204 (response :status)) (throw+ {:type      :not-found
                                            :errorCode 204
                                            }))
  (when (= 405 (response :status)) (throw+ {:type      :Error
                                            :errorCode 405
                                            }))
  (when (= 500 (response :status)) (throw+ {:type      :not_primary
                                            :errorCode 405
                                            }))
  (when (and (= "[]" (response :body)) (= "" (response :body))) (throw+ {:type :missing-body
                                                                         :response response
                                                                         } ))

  ((first (json/parse-string (response :body) true)) :Value)
  )


(defn get*
  ([client key]
   (get* client key {}))
  ([client key opts]

   (->> opts
        (http/get (url client RDuri key))
        parse))
  )

(defn get
  "Gets the current value of a key. If the key does not exist, returns nil.
  Single-node queries return the value of the node itself: a string for leaf
  nodes; a sequence of keys for a directory.
  Options:"
  ([client key]
   (get* client key {}))

  ([client key opts]
   (try+
     (get* client key opts)

     (catch [:status 404] _ nil))))

(defn write
  "Resets the current value of a given key to `value`. Options:

  :ttl
  :timeout"
  ([client key value]
   (write client key value {}))
  ([client key value opts]
   (->> (assoc opts :value value)
        (http-opts client)

        (http/put (url client RDuri key value))
        parse)))

(defn create!*
  ([client key value]
   (create!* client key value {}))
  ([client key value opts]
   (->> (assoc opts :value value)
        (http-opts client)
        (http/put (url client RDuri key value))
        parse)))

(defn create
  "Creates a new, automatically named object under the given key with the
  given value, and returns the full key of the created object. Options:

  :timeout
  :ttl"
  ([client key value]
   (create client key value {}))
  ([client key value opts]
   (-> (create!* client key value opts)
       :node
       :key)))

(defn delete
  "Deletes the given key"
  ([client key]
   (delete client key {}))
  ([client key opts]
   (->> (http/delete (url client RDuri key))
        parse)))

;(defn delete-all
;  "Deletes all nodes, recursively if necessary, under the given directory.
;  Options:
;
;  :timeout"
;  ([client key]
;   (delete-all client key {}))
;  ([client key opts]
;   (doseq [node (->> (select-keys opts [:timeout])
;                     (get* client key)
;                     :node
;                     :nodes)]
;     (delete client (:key node) {:recursive? (:dir node)
;                                 :timeout    (:timeout opts)}))))

(defn cas
  "Compare and set based on the current value. Updates key to be value' iff the
  current value of key is value. Optionally, you may also constrain the
  previous index and/or the existence of the key. Returns false for CAS failure."
  ([client key value value']
   (cas client key value value' {}))
  ([client key value value' opts]
   (try+
     (->> (http/put (url client RDuri key value value'))
          parse)
     (catch [:errorCode 404] _ false)

     )))

(defn enqueue*
  ([client key]
   (enqueue* client key {}))
  ([client key opts]

   (->> (http/put (url client Quri key))))
  )

(defn enqueue
  ([client key]
   (enqueue* client key {}))

  ([client key opts]
   (try+
     (enqueue* client key opts)
     (catch [:status 404] _ nil)
     (catch [:status 200] _ true))))



(defn dequeue*
  ([client]
   (dequeue* client {}))
  ([client opts]

   (->> opts
        (http/delete (url client Quri))
        parse))
  )

(defn dequeue
  ([client]
   (dequeue* client {}))

  ([client opts]
   (try+
     (dequeue* client opts)
     (catch [:status 404] _ nil))))


(defn queuepeek*
  ([client]
   (queuepeek* client {}))
  ([client opts]

   (->> opts
        (http/get (url client Quri "peek"))
        parse))
  )

(defn queuepeek
  ([client]
   (queuepeek* client {}))

  ([client opts]
   (try+
     (queuepeek* client opts)
     (catch [:status 404] _ nil))))

(defn queuecount*
  ([client]
   (queuecount* client {}))
  ([client opts]

   (->> opts
        (http/get (url client Quri))
        parse))
  )

(defn queuecount
  ([client]
   (queuecount* client {}))

  ([client opts]
   (try+
     (queuecount* client opts)
     (catch [:status 404] _ nil))))
