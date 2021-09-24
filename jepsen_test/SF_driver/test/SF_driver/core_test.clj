(ns SF_driver.core-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [clj-http.client :as http]
            [cheshire.core :refer :all]
            [SF_driver.core :as sfc]))

(def c (sfc/connect "http://jepsen.northeurope.cloudapp.azure.com:35112/api"))

; Delete all data before each test
;(use-fixtures :each #(do (sfc/delete-all! c nil) (%)))

(deftest remap-keys-test
  (is (= (sfc/remap-keys inc {1 2 3 4})
         {2 2 4 4})))

(deftest key-encoding-test
  (testing "nil"
    (is (= "" (sfc/encode-key nil))))

  (testing "simple strings"
    (is (= "foo" (sfc/encode-key "foo"))))

  (testing "strings with slashes"
    (is (= "foo/bar" (sfc/encode-key "foo/bar"))))

  (testing "unicode"
    (is (= "%E2%88%B4/%E2%88%8E" (sfc/encode-key "∴/∎"))))

  (testing "keywords"
    (is (= "foo" (sfc/encode-key :foo))))

  (testing "symbols"
    (is (= "foo" (sfc/encode-key 'foo))))

  (testing "sequences"
    (is (= "foo" (sfc/encode-key [:foo])))
    (is (= "foo/bar" (sfc/encode-key [:foo :bar])))
    (is (= "foo/bar" (sfc/encode-key '(:foo :bar))))
    (is (= "foo/bar/baz" (sfc/encode-key ["foo/bar" "baz"])))))

(deftest reset-get-test
  (testing "a simple key"
    (sfc/reset! c "test" 10)
    (is (= 10 (sfc/get c "test")))
    )

  (testing "Paths and unicode"
    (sfc/reset! c "ℵ" 20)
    (is (= 20 (sfc/get c "ℵ")))
    )
  )

(deftest puthttp
  (testing "Does the testput?"
    (is(= 200 ((http/put "http://jepsen.northeurope.cloudapp.azure.com:35112/api/ReliableDictionary/rand/10"):status)))
    ;(print (sfc/get c "rand"))
    (is (= 10 (sfc/get c "rand")))
    )
  )


(deftest missing-values
  (is (false? (sfc/get c "SDFSDFSDFSD:nonexistent"))))

(deftest create-test!
      (println (sfc/create! c "randkey1337" 123123))
)

(deftest cas-test!
  (sfc/reset! c "foo" 0)
  (is (= 0 (sfc/get c "foo")))
  (is (false? (sfc/cas! c "foo" 5 5)))
  (is (= 0 (sfc/get c "foo")))

  (sfc/cas! c "foo" 52 0)
  (is (= 52 (sfc/get c "foo"))))




; Queue


;(deftest queue-cycle-test
;  (testing "Testing the cycle of the queue."
;    (sfc/enqueue c 12312312312)
;    ;(is (= 1 (sfc/queuecount c)))
;    (is (= 12312312312 (sfc/queuepeek c)))
;    (is (= 12312312312 (sfc/dequeue c)))
;    )
;  )
