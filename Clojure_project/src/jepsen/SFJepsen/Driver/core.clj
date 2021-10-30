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
            [clojure.data.json :as json2]
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
  ;(str "http://" (:endpoint clientaddress) ":35112/api")
  (str "http://10.0.0.7:35112/api")
  )


(defn primarybase-url
  "Constructs the base URL for all etcd requests. Example:

  (base-url client) ; => \"http://127.0.0.1:4001/v2\""
  []
  ;(str "http://" (:endpoint clientaddress) ":35112/api")
  (str "http://10.0.0.6:35112/api"))


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

(defn ^String primaryurl
  "The URL for a key under a specified root-key.
  (url client [\"keys\" \"foo\"]) ; => \"http://127.0.0.1:4001/v2/keys/foo"
  ([client uri]
   (str (primarybase-url) "/" uri))
  ([client uri key-seq]
   (str (primarybase-url) "/" uri "/" key-seq))
  ([client uri key value]
   (str (primarybase-url) "/" uri "/" key "/" value))
  ([client uri key value1 value2]
   (str (primarybase-url) "/" uri "/" key "/" value1 "/" value2)))

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
  (when (= 204 (response :status)) (throw+ {:stutus   204
                                            :type     :missing-value
                                            :response response
                                            }))
  (when (and (= "[]" (response :body)) (= "" (response :body))) (throw+ {:type     :missing-body
                                                                         :response response
                                                                         }))
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
   (get client key {}))

  ([client key opts]

   (get* client key opts)
   ))


(defn write
  "Resets the current value of a given key to `value`. Options:

  :ttl
  :timeout"
  ([client key value]
   (write client key value {}))
  ([client key value opts]
   (->> (assoc opts :value value)
        (http-opts client)
        ;(info (url client RDuri key value))
        (http/put (primaryurl client RDuri key value))
        parse)
   ;(catch [:status 500] _ :type :not_primary :errorCode 405)
   ))

(defn create!*
  ([client key value]
   (create!* client key value {}))
  ([client key value opts]
   (->> (assoc opts :value value)
        (http-opts client)
        (http/put (primaryurl client RDuri key value))
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
   (->> (http/delete (primaryurl client RDuri key))
        parse)))

(defn cas
  "Compare and set based on the current value. Updates key to be value' iff the
  current value of key is value. Optionally, you may also constrain the
  previous index and/or the existence of the key. Returns false for CAS failure."
  ([client key value value']
   (cas client key value value' {}))
  ([client key value value' opts]

   (->> (http/put (primaryurl client RDuri key value value'))
        parse)
   ))

(defn enqueue*
  ([client key]
   (enqueue* client key {}))
  ([client key opts]

   (->> (http/put (primaryurl client Quri key))))
  )

(defn enqueue
  ([client key]
   (enqueue* client key {}))

  ([client key opts]

   (enqueue* client key opts)
   ))



(defn dequeue*
  ([client]
   (dequeue* client {}))
  ([client opts]

   (->> opts
        (http/delete (primaryurl client Quri))
        parse))
  )

(defn dequeue
  ([client]
   (dequeue* client {}))

  ([client opts]

   (dequeue* client opts)
   ))


(defn queuepeek*
  ([client]
   (queuepeek* client {}))
  ([client opts]

   (->> opts
        (http/get (primaryurl client Quri "peek"))
        parse))
  )

(defn parsetxntojson [txn]
  (json2/write-str {:transaction txn})
  )

(defn parsejsontotxn [json]
  (json2/read-str json)
  )


(defn txntourl [client uri query]
  (str (base-url client) "/" uri "?query=" query)
  )



(defn parsetxn [transaction resultmap]
  "gets a http response and parses the body to a map


  Body : [{\"Key\":\"thekey\",\"Value\":\"3\"},{\"Key\":\"thekey\",\"Value\":\"3\"},{\"Key\":\"thekey\",\"Value\":\"True\"},{\"Key\":\"thekey\",\"Value\":\"15\"},{\"Key\":\"thekey\",\"Value\":\"True\"}][\\r][\\n]\"

  return value: [{\"Key\" \"thekey\", \"Value\" \"3\"} {\"Key\" \"thekey\", \"Value\" \"3\"} {\"Key\" \"thekey\", \"Value\" \"True\"} {\"Key\" \"thekey\", \"Value\" \"15\"} {\"Key\" \"thekey\", \"Value\" \"True\"}]

  "

  (parsejsontotxn (:body resultmap))
  )


(defn parseresult [r]
  (if (= (val (second r)) "False")
    nil
    (if (clojure.string/includes? (val (second r)) "System.TimeoutException:") (throw+ {:status   601
                                                                                 :type     :RealiableCollectionslockTimeout
                                                                                 :response (val (second r))
                                                                                 }) (Long/parseLong (val (second r))))

    )

  )


(defn Clojuremaptojsonoperation
  [operation]

  (case (first operation)
    :r
    {:operation "r"
     :key       (str (second operation))}

    :w
    {:operation "w"
     :key       (str (second operation))
     :value     (nth operation 2)}

    :cas
    {:operation "cas"
     :key       (str (second operation))
     :value     (nth operation 2)
     :exspected (nth operation 3)}

    :d
    {:operation "d"
     :key       (str (second operation))}

    :enqueue
    {:operation "enqueue"
     :value     (nth operation 2)}

    :dequeue
    {:operation "dequeue"
     :value     (nth operation 1)}

    :peek
    {:operation "peek"}

    :count
    {:operation "coundcount"}

    :drain
    {:operation "drain"}

    )

  )

(defn txn [client, transaction]
  "Gets a client a list a transactions in a map
  [:r 1039 nil]
  {\"operation\":\"w\",
  \"key\":\"thekey\",
  \"value\":3}

  http://10.0.0.5:35112/api/ReliableDictionary?query={\"transaction\":[{\"operation\":\"w\",\"key\":\"thekey\",\"value\":3},{\"operation\":\"r\",\"key\":\"thekey\"},{\"operation\":\"c\",\"key\":\"thekey\",\"value\":15,\"expected\":3},{\"operation\":\"r\",\"key\":\"thekey\"},{\"operation\":\"d\",\"key\":\"thekey\"}]}
  "
  (parsetxn transaction (http/put (txntourl client RDuri (parsetxntojson (mapv Clojuremaptojsonoperation transaction)))))

  )

(defn queuepeek
  ([client]
   (queuepeek* client {}))

  ([client opts]

   (queuepeek* client opts)
   ))

(defn queuecount*
  ([client]
   (queuecount* client {}))
  ([client opts]

   (->> opts
        (http/get (primaryurl client Quri))
        parse))
  )

(defn queuecount
  ([client]
   (queuecount* client {}))

  ([client opts]

   (queuecount* client opts)
   ))
