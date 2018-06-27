(ns com.gfredericks.how-to-ns.main-test
  (:require
   [clojure.test                   :refer [deftest is testing]]
   [com.gfredericks.how-to-ns.main :as how-to-ns-main])
  (:import
   (java.io                 File)
   (java.nio.file           Files)
   (java.nio.file.attribute FileAttribute)))

(defmacro with-err-str
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*err* s#]
       (let [ret# (do ~@body)]
         [ret# (str s#)]))))

(def unreadable-code
  "(ns by-jove (:require #_#_#_))\n\n(defn foo-bar [a] (inc a))\n")

(deftest unreadable-ns-test
  (let [d (Files/createTempDirectory "how-to-ns-tests" (make-array FileAttribute 0))
        f (File. (str d) "bad-ns.clj")]
    (try
      (spit f unreadable-code)
      (testing "check"
        (let [[_ err] (with-err-str
                        (how-to-ns-main/check [(str d)] {}))]
          (is (re-find #"Unreadable ns form.*bad-ns\.clj" err)
              "prints the filename")
          (is (re-find #"Unmatched delimiter" err)
              "prints the original reader error")))
      (testing "fix"
        (let [[caught err]
              (with-err-str
                (try
                  (how-to-ns-main/fix [(str d)] {})
                  nil
                  (catch Exception e
                    e)))]
          (is (re-find #"Unreadable ns form.*bad-ns\.clj" err)
              "prints the filename")
          (is (re-find #"Unmatched delimiter" err)
              "prints the original reader error")
          (is caught
              "threw an exception")
          (is (= unreadable-code (slurp f))
              "Didn't change the file")))
      (finally
        (.delete f)
        (Files/delete d)))))
