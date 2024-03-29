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
(def openque [[:qe 9992886] [:qe 9992886]])
(def opdeque [[:qd nil] [:qd nil]])
(def opappenwrite [[:w 9992886 [31]] [:w 9992887 [32, 32]]])
(def opappenread [[:r 9992886] [:r 9992887]])
(def opappenappend [[:append 9992886 1] [:append 9992886 2] [:append 9992886 3] [:append 9992887 1] [:append 9992887 2] [:append 9992887 3] [:append 9992887 4]])


(def opappenappendtest [[:append 9992886 1] [:r 9992886] [:r 9992887] [:append 9992886 2] [:append 9992886 3] [:r 9992886] [:r 9992887] [:append 9992887 1] [:append 9992887 2] [:append 9992887 3] [:r 9992886] [:r 9992887] [:append 9992887 4]])

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


(deftest txn
  "   Id: int
  Type: str
  Values:
    key: string
    Value: int
    Expected: int
  Return: future return value that can be used and compares to history. "

  (testing "TXN Write RDI"
    (is (= [{"Key" "9992886", "Value" "31"} {"Key" "9992887", "Value" "1"}] (sfc/txn c sfc/RDuri opw)))

    )

  (testing "TXN read RDI"
    (is (= [{"Key" "9992886", "Value" "31"}] (sfc/txn c sfc/RDuri opr)))

    )

  (testing "TXN CAS RDI"

    (is (= [{"Key" "9992886", "Value" "31"} {"Key" "cas", "Value" "Failed"}] (sfc/txn c sfc/RDuri opcas)))
    )

  (testing "TXN mix RDI"

    (is (= [{"Key" "9992886", "Value" "31"} {"Key" "9992886", "Value" "31"} {"Key" "9992887", "Value" "1"}] (sfc/txn c sfc/RDuri opa)))
    )


  (testing "TXN + parse result RDI"
    (is (= [[:r 999127 nil] [:w 999123 1] [:w 999126 3] [:r 999127 nil] [:r 999123 1]] (->> (sfc/txn c sfc/RDuri (:value op))
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

  (testing "TXN enqueue RDQ"

    (is (= [{"Key" "qe", "Value" "9992886"} {"Key" "qe", "Value" "9992886"}] (sfc/txn c sfc/RQuri openque)))
    )



  (testing "TXN deque RDq"

    (is (= [{"Key" "qd", "Value" "9992886"} {"Key" "qd", "Value" "9992886"}] (sfc/txn c sfc/RQuri opdeque)))
    )



  (testing "TXN write RDread"

    (is (= [{"Key" "9992886", "Value" [31]} {"Key" "9992887", "Value" [32 32]}] (sfc/txn c sfc/RAuri opappenwrite)))
    )


  (testing "TXN append RDread"

    (is (= [{"Key" "9992886", "Value" [31]} {"Key" "9992887", "Value" [32 32]}] (sfc/txn c sfc/RAuri opappenread)))
    )

  (testing "TXN read RDappend"

    (is (= [{"Key" "9992886", "Value" [31 1]} {"Key" "9992886", "Value" [31 1 2]} {"Key" "9992886", "Value" [31 1 2 3]} {"Key" "9992887", "Value" [32 32 1]}
            {"Key" "9992887", "Value" [32 32 1 2]} {"Key" "9992887", "Value" [32 32 1 2 3]} {"Key" "9992887", "Value" [32 32 1 2 3 4]}] (sfc/txn c sfc/RAuri opappenappend)))
    )

  (testing "TXN read RDappendorder"

    (is (= [{"Key" "9992886", "Value" [31 1 2 3 1]} {"Key" "9992886", "Value" [31 1 2 3 1]} {"Key" "9992887", "Value" [32 32 1 2 3 4]} {"Key" "9992886", "Value" [31 1 2 3 1 2]}
            {"Key" "9992886", "Value" [31 1 2 3 1 2 3]} {"Key" "9992886", "Value" [31 1 2 3 1 2 3]} {"Key" "9992887", "Value" [32 32 1 2 3 4]} {"Key" "9992887", "Value" [32 32 1 2 3 4 1]}
            {"Key" "9992887", "Value" [32 32 1 2 3 4 1 2]} {"Key" "9992887", "Value" [32 32 1 2 3 4 1 2 3]} {"Key" "9992886", "Value" [31 1 2 3 1 2 3]}
            {"Key" "9992887", "Value" [32 32 1 2 3 4 1 2 3]} {"Key" "9992887", "Value" [32 32 1 2 3 4 1 2 3 4]}] (sfc/txn c sfc/RAuri opappenappendtest)))
    )

  )



(deftest txnparse
  (testing "txntojson"

    (is (= "1" (sfc/parsetxntojson (mapv sfc/Clojuremaptojsonoperation opappenappendtest))))

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

