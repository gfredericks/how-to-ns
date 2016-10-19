(ns leiningen.how-to-ns-test
  (:require [clojure.test :refer :all]
            [leiningen.how-to-ns :as how-to-ns]))

(defn correctly-formatted?
  [ns-str opts]
  (= ns-str (how-to-ns/reformat-ns-str ns-str opts)))

(defn ->opts
  [& kv-pairs]
  (if (seq kv-pairs)
    (apply assoc how-to-ns/default-opts kv-pairs)
    how-to-ns/default-opts))

(deftest it-works
  (are [ns-str opts] (correctly-formatted? ns-str opts)
    "(ns thomas)" (->opts)
    "(ns thomas
  \"here's my
  docstring\")" (->opts)

"(ns com.example.my-application.server
  \"Example application HTTP server and routing.\"
  (:refer-clojure :exclude [send])
  (:require
   [clojure.core.async :as async :refer [<! <!! >! >!!]]
   [com.example.my-application.base]
   [com.example.my-application.server.sse :as server.sse]
   [io.pedestal.http :as http]
   [io.pedestal.http.sse :as http.sse]
   [ring.util.response :as response])
  (:import
   (java.nio.file Files LinkOption)
   (org.apache.commons.io FileUtils)))"
(->opts :align-clauses? false))

  (are [ns-str opts] (not (correctly-formatted? ns-str opts))
    ;; total garbage
    "(ns thomas thomas)" (->opts)

    ;; needs docstring
    "(ns thomas)" (->opts :require-docstring? true)

    ;; clauses in wrong order
    "(ns thomas
  (:import
   (java.util Random))
  (:require
   [clj-time.core]))"
    (->opts)

    ;; unparenthesized clauses
    "(ns thomas.disney
  (:require
   clj-time.core))"
    (->opts)

    ))
