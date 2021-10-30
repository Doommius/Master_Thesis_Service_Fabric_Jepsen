(ns jepsen.SFJepsen.Driver.core_test
  (:require [clojure.test :refer :all]
    ;[clojure.tools.logging :refer [debug info warn]]
            [cheshire.core :refer :all]
            [jepsen.SFJepsen.Driver.core :as sfc]))

(def c (sfc/connect "10.0.0.6"))
(def op {:type :invoke, :f :txn, :value [[:r 999127 nil] [:w 999123 1] [:w 999126 3] [:r 999127 nil] [:r 999123 nil]], :time 2557245142, :process 7})
(def opa [[:w 9992886 31] [:r 9992886 nil] [:w 9992887 1]])
(def opr [[:r 9992886 nil]])
(def opw [[:w 9992886 31] [:w 9992887 1]])
(def opcas [[:w 9992886 31] [:cas 9992886 1 31]])
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



(deftest read_Test_fail

  (testing "read_Test_fail"
    (is (= nil (sfc/get c "ℵ123")))
    )
  )

(deftest txn
  "   Id: int
  Type: str
  Values:
    key: string
    Value: int
    Expected: int
  Return: future return value that can be used and compares to history. "
  (testing "TXN read"
    (is (= [{"Key" "2886", "Value" "31"}] (sfc/txn c opr)))

    )
  (testing "TXN Write"
    (is (= [{"Key" "2886", "Value" "31"} {"Key" "2887", "Value" "1"}] (sfc/txn c opw)))

    )

  (testing "TXN CAS"

    (is (= [{"Key" "2886", "Value" "31"} {"Key" "cas", "Value" "Failed"}] (sfc/txn c opcas)))
    )

  (testing "TXN MAX"

    (is (= [{"Key" "2886", "Value" "31"} {"Key" "2886", "Value" "31"} {"Key" "2887", "Value" "1"}] (sfc/txn c opa)))
    )



  (testing "TXN + parse result"
    (is (= [[:r 127 nil] [:w 123 1] [:w 126 3] [:r 127 nil] [:r 123 1]] (->> (sfc/txn c (:value op))
                (mapv (fn [[f k v] r]
                        [f k (case f
                               :r (sfc/parseresult r)
                               :w (sfc/parseresult r)
                               :cas (sfc/parseresult r)
                               :d (sfc/parseresult r)
                               :a (sfc/parseresult r)
                               :append v)])
                      (:value op))
                )))
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

