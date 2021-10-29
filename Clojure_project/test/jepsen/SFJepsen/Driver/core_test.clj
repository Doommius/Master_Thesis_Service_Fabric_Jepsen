(ns jepsen.SFJepsen.Driver.core_test
  (:require [clojure.test :refer :all]
    ;[clojure.tools.logging :refer [debug info warn]]
            [cheshire.core :refer :all]
            [jepsen.SFJepsen.Driver.core :as sfc]))

(def c (sfc/connect "10.0.0.5"))
;(def c (sfc/connect "jepsen.northeurope.cloudapp.azure.com"))




; Delete all data before each test
;(use-fixtures :each #(do (sfc/delete-all! c nil) (%)))


(deftest write-get-test
  (testing "a simple key"
    (sfc/write c "test" 10)
    (is (= 10 (sfc/get c "test")))
    )

  (testing "Paths and unicode"
    (sfc/write c "ℵ" 20)
    (is (= 20 (sfc/get c "ℵ")))
    )
  )

(deftest puthttp
  (testing "Does the testput?"

    (is (= (str "http://" (:endpoint c) ":35112/api") (sfc/base-url c)))

    (is (= 10 (sfc/write c "rand" 10)))
    ;(print (sfc/get c "rand"))
    (is (= 10 (sfc/get c "rand")))
    )
  )


;(deftest missing-values
;  (is (false? (sfc/get c "SDFSDFSDFSD:nonexistent"))))


(deftest cas-test
  (is (= 0 (sfc/write c "casfoo" 0)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 0 (sfc/cas c "casfoo" 5 10)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 5 (sfc/write c "casfoo" 5)))
  (is (= 52 (sfc/cas c "casfoo" 52 5)))
  (is (= 52 (sfc/get c "casfoo")))
  (is (= 0 (sfc/write c "casfoo" 0)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 0 (sfc/cas c "casfoo" 5 10)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 5 (sfc/write c "casfoo" 5)))
  (is (= 52 (sfc/cas c "casfoo" 52 5)))
  (is (= 52 (sfc/get c "casfoo")))
  (is (= 0 (sfc/write c "casfoo" 0)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 0 (sfc/cas c "casfoo" 5 10)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 5 (sfc/write c "casfoo" 5)))
  (is (= 52 (sfc/cas c "casfoo" 52 5)))
  (is (= 52 (sfc/get c "casfoo")))
  (is (= 0 (sfc/write c "casfoo" 0)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 0 (sfc/cas c "casfoo" 5 10)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 5 (sfc/write c "casfoo" 5)))
  (is (= 52 (sfc/cas c "casfoo" 52 5)))
  (is (= 52 (sfc/get c "casfoo")))
  (is (= 0 (sfc/write c "casfoo" 0)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 0 (sfc/cas c "casfoo" 5 10)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 5 (sfc/write c "casfoo" 5)))
  (is (= 52 (sfc/cas c "casfoo" 52 5)))
  (is (= 52 (sfc/get c "casfoo")))
  (is (= 0 (sfc/write c "casfoo" 0)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 0 (sfc/cas c "casfoo" 5 10)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 5 (sfc/write c "casfoo" 5)))
  (is (= 52 (sfc/cas c "casfoo" 52 5)))
  (is (= 52 (sfc/get c "casfoo")))
  (is (= 0 (sfc/write c "casfoo" 0)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 0 (sfc/cas c "casfoo" 5 10)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 5 (sfc/write c "casfoo" 5)))
  (is (= 52 (sfc/cas c "casfoo" 52 5)))
  (is (= 52 (sfc/get c "casfoo")))
  (is (= 0 (sfc/write c "casfoo" 0)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 0 (sfc/cas c "casfoo" 5 10)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 5 (sfc/write c "casfoo" 5)))
  (is (= 52 (sfc/cas c "casfoo" 52 5)))
  (is (= 52 (sfc/get c "casfoo")))
  (is (= 0 (sfc/write c "casfoo" 0)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 0 (sfc/cas c "casfoo" 5 10)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 5 (sfc/write c "casfoo" 5)))
  (is (= 52 (sfc/cas c "casfoo" 52 5)))
  (is (= 52 (sfc/get c "casfoo")))
  (is (= 0 (sfc/write c "casfoo" 0)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 0 (sfc/cas c "casfoo" 5 10)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 5 (sfc/write c "casfoo" 5)))
  (is (= 52 (sfc/cas c "casfoo" 52 5)))
  (is (= 52 (sfc/get c "casfoo")))
  (is (= 0 (sfc/write c "casfoo" 0)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 0 (sfc/cas c "casfoo" 5 10)))
  (is (= 0 (sfc/get c "casfoo")))
  (is (= 5 (sfc/write c "casfoo" 5)))
  (is (= 52 (sfc/cas c "casfoo" 52 5)))
  (is (= 52 (sfc/get c "casfoo"))))


(deftest read_Test_fail

  (testing "read_Test_fail"
    (is (= nil (sfc/get c "ℵ123")))
    )
  )


(defn txnpayload []
  (sfc/parsejsontotxn "{\"transaction\":[
                                {\"operation\":\"w\", \"key\":\"thekey\",\"value\":3},
                                {\"operation\":\"r\",\"key\":\"thekey\"},
                                {\"operation\":\"c\",\"key\":\"thekey\",\"value\":15,\"expected\":3},
                                {\"operation\":\"r\",\"key\":\"thekey\"},
                                {\"operation\":\"d\",\"key\":\"thekey\"}
                                ]}")
  )

(deftest txn
  "   Id: int
  Type: str
  Values:
    key: string
    Value: int
    Expected: int
  Return: future return value that can be used and compares to history. "
  (testing "parse txn test"
    (print (txnpayload))
    (print (sfc/parsetxntojson (txnpayload)))
    )

  (testing "txn test"

    (is (= [{"Key" "thekey", "Value" "3"} {"Key" "thekey", "Value" "3"} {"Key" "thekey", "Value" "True"} {"Key" "thekey", "Value" "15"} {"Key" "thekey", "Value" "True"}] (sfc/txn c (txnpayload))))
    )
  )

;Queue


;(deftest queue-cycle-test
;  (testing "Addeding item to queue"
;    (info (sfc/enqueue c 12312312312))
;    )
;
;  (testing "checking queue count."
;    (is (= 1 (sfc/queuecount c)))
;    )
;
;  (testing "Dequeueing"
;    (is (= 12312312312 (sfc/dequeue c)))
;    )
;  )

