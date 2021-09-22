(defproject jepsen.disque "0.1.0-SNAPSHOT"
  :description "Jepsen test for Disque"
  :url "https://github.com/aphyr/jepsen"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [jepsen "0.1.17"]
                 [com.github.xetorthio/jedisque "0.0.4"]]
  :main jepsen.disque)
