(defproject jepsen.sf "0.5.0-SF-Jepsentest"
  :description "Jepsen tests for SF"
  :url "https://github.com/aphyr/jepsen"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [jepsen "0.2.4"]
                 [base64-clj "0.1.1"]
                 [clj-http "3.12.3"]
                 [cheshire "5.10.1"]]
  :repl-options {:init-ns jepsen.sf}
  :main jepsen.sf)
