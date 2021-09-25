(ns jepsen.SFJepsen.Driver.core_test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :refer [debug info warn]]
            [cheshire.core :refer :all]
            [jepsen.SFJepsen.Driver.core :as sfc]))

(def c (sfc/connect "jepsen.northeurope.cloudapp.azure.com" {:timeout 100}))

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


(deftest missing-values
  (is (false? (sfc/get c "SDFSDFSDFSD:nonexistent"))))

(deftest create-test!
  (println (sfc/create c "randkey1337" 123123))
  )

(deftest cas-test!
  (is (= 0 (sfc/write c "foo" 0)))
  (is (= 0 (sfc/get c "foo")))
  (is (= 0 (sfc/cas c "foo" 5 10)))
  (is (= 0 (sfc/get c "foo")))
  (is (= 5 (sfc/write c "foo" 5)))
  (is (= 52 (sfc/cas c "foo" 52 5)))
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
