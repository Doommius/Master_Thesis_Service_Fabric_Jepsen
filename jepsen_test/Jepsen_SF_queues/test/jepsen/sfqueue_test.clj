(ns jepsen.sfqueue_test
    (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [jepsen [core :as jepsen]
                    [report :as report]]))

(deftest partitions
  (let [test (jepsen/run! (sfqueue/partitions-test))]
    (is (:valid? (:results test)))))
