(defproject jepsen.servicefabric "0.1.0-SNAPSHOT"
   :description "A Jepsen test for Service Fabric"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :main jepsen.servicefabric
    :dependencies [[org.clojure/clojure "1.10.0"]
                   [jepsen "0.2.1-SNAPSHOT"]
                   [verschlimmbesserung "0.1.3"]])
