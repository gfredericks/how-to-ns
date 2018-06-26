(defproject com.gfredericks/how-to-ns "0.2.1"
  :description "A Clojure linter for Stuart Sierra's how-to-ns standard"
  :url "https://github.com/gfredericks/how-to-ns"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[com.googlecode.java-diff-utils/diffutils "1.2.1"]]
  :deploy-repositories [["releases" :clojars]]
  :vcs :git
  :profiles
  {:provided
   {:dependencies [[org.clojure/clojure "1.9.0"]]}
   :dev
   {:dependencies [[org.clojure/test.check "0.10.0-alpha3"]]}})
