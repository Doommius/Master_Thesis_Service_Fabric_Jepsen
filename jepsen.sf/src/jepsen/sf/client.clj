(ns jepsen.sf.client
  (:require [clojure.tools.logging :refer [debug info warn]]
            [clojure.string :as str]
            [jepsen.client :as client]
            [jepsen.control.net :as net]
            [base64-clj.core :as base64]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [dom-top.core :refer [with-retry]]
            [slingshot.slingshot :refer [try+ throw+]]))

(defn maybe-int [value]
  (if (= value "null")
    nil
    (Integer. value)))


(defn parse-body
  "Parse the base64 encoded value.
   The response JSON looks like:
    [
     {
       \"CreateIndex\": 100,
       \"ModifyIndex\": 200,
       \"Key\": \"foo\",
       \"Flags\": 0,
       \"Value\": \"YmFy\"
     }
    ]
  "
  [resp]
  (let [body  (-> resp
                  :body
                  (json/parse-string #(keyword (.toLowerCase %)))
                  first)
        value (-> body :value base64/decode maybe-int)]
    (assoc body :value value)))


(defn get
  ([url]
   (http/get url))
  ([url key]
   (http/get (str url key)))
  ([url key consistency]
   (http/get (str url key)
             {:query-params {(keyword consistency) nil}})))

(defn put!
  ([url key value]
   (http/put (str url key value)))
  ([url key value consistency]
   (http/put (str url key value)
             {:query-params {(keyword consistency) nil}})))

(defn cas!
  ([url key value new-value]
   (http/put (str url key value new-value)))
  ([url key value new-value consistency]
   (http/put (str url key value new-value consistency)
             {:query-params {(keyword consistency) nil}})))
(defn txn
  "TODO Model txn requests when we get to testing that part of sf"
  [])

(defmacro with-errors
  [op idempotent & body]
  `(try ~@body
    (catch Exception e#
      (let [type# (if (~idempotent (:f ~op))
                    :fail
                    :info)]
        (condp re-find (.getMessage e#)
          #"404" (assoc ~op :type type# :error :key-not-found)
          #"403" (assoc ~op :type type# :error :not-authorized)
          #"500" (assoc ~op :type type# :error :server-unavailable)
          (throw e#))))))

