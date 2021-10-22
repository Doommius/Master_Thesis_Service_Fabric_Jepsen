(ns elle_checker)

(require '[elle.list-append :as a])


(def h [{:type :ok, :value [[:append :x 1] [:r :y [1]]]}
        {:type :ok, :value [[:append :x 2] [:append :y 1]]}
        {:type :ok, :value [[:r :x [1 2]]]}])


(pprint (a/check {:consistency-models [:serializable], :directory "out"} h))
{:valid? false,
 :anomaly-types (:G1c),
 :anomalies
 {:G1c
  [{:cycle
          [{:type :ok, :value [[:append :x 2] [:append :y 1]]}
           {:type :ok, :value [[:append :x 1] [:r :y [1]]]}
           {:type :ok, :value [[:append :x 2] [:append :y 1]]}],
    :steps
          ({:type :wr, :key :y, :value 1, :a-mop-index 1, :b-mop-index 1}
           {:type :ww,
            :key :x,
            :value 1,
            :value' 2,
            :a-mop-index 0,
            :b-mop-index 0}),
    :type :G1c}]},
 :not #{:read-committed},
 :also-not
 #{:consistent-view :cursor-stability :forward-consistent-view
   :monotonic-atomic-view :monotonic-snapshot-read :monotonic-view
   :repeatable-read :serializable :snapshot-isolation
   :strict-serializable :update-serializable}}