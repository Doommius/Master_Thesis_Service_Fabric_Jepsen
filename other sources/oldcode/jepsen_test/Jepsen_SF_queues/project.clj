(defproject jepsen.sfqueue "0.1.0-SNAPSHOT"
            :description "Jepsen test for SF relaible Queues"
            :url "https://github.com/aphyr/jepsen"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.10.0"]
                           [jepsen "0.2.4"]
                           [clj-http "3.12.3"]]

            :repl-options {:init-ns jepsen.sf}
            :main jepsen.sfqueue.runner
            )

