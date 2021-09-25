(defproject jepsen.SFJepsen "0.1.0-SNAPSHOT"
            :description "Jepsen test for SF relaible Queues"
            :url "https://github.com/aphyr/jepsen"
            :jvm-opts ["-Djava.awt.headless=true"]
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.10.3"]
                           [jepsen "0.2.4"]
                           [cheshire "5.9.0"]
                           [clj-http "3.12.3"]]

            :repl-options {:init-ns jepsen.SFJepsen}
            :main jepsen.SFJepsen.runner
            )

