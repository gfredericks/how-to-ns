(ns leiningen.how-to-ns-test
  (:require [clojure.test :refer :all]
            [com.gfredericks.how-to-ns :as how-to-ns]))

(defn correctly-formatted?
  [ns-str opts]
  (= ns-str (how-to-ns/reformat-ns-str ns-str opts)))

(defn ->opts
  [& kv-pairs]
  (if (seq kv-pairs)
    (apply assoc how-to-ns/default-opts kv-pairs)
    how-to-ns/default-opts))

(deftest it-works
  (are [goodbad opts ns-str]
      ((case goodbad :good identity :bad not)
       (correctly-formatted? ns-str opts))

    :good ; stu's example
    (->opts)
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

    :good ; simplest
    (->opts :require-docstring? false)
    "(ns thomas)"

    :good ; with a required docstring
    (->opts)
    "(ns thomas
  \"here's my
  docstring\")"

    :bad ; total garbage
    (->opts)
    "(ns thomas thomas)"

    :bad ; needs docstring
    (->opts)
    "(ns thomas)"

    :bad ; clauses in wrong order
    (->opts)
    "(ns thomas
  \"docstring\"
  (:import
   (java.util Random))
  (:require
   [clj-time.core]))"

    :bad ; unparenthesized clauses
    (->opts)
    "(ns thomas.disney
  \"docstring\"
  (:require
   clj-time.core))"

    :bad ; import using square brackets
    (->opts)
    "(ns thomas.disney
  \"docstring\"
  (:import
   [java.util Date]))"

    :good ; import using square brackets when allowed
    (->opts :import-square-brackets? true)
    "(ns thomas.disney
  \"docstring\"
  (:import
   [java.util Date]))"

    :bad ; requires using parentheses
    (->opts)
    "(ns thomas
  \"docstring\"
  (:require
   (clojure.data.xml)))"

    :good ; :refer [not all]
    (->opts)
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer [not-all]]))"

    :bad ; :refer :all
    (->opts)
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer :all]))"

    :good ; :refer :all when allowed
    (->opts :allow-refer-all? true)
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer :all]))"

    :bad ; :rename
    (->opts)
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer [deftest] :rename {deftest tefdest}]))"

    :good ; :allow-rename?
    (->opts :allow-rename? true)
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer [deftest] :rename {deftest tefdest}]))"

    :bad ; two requires
    (->opts)
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test])
  (:require
   [clojure.data.xml]))"

    :bad ; renames
    (->opts)
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer [is] :rename {is isn't}]))"

    :bad ; use
    (->opts)
    "(ns thomas
  \"docstring\"
  (:use [clojure.test]))"

    :bad ; require instead of :require
    (->opts)
    "(ns thomas
  \"docstring\"
  (require [clojure.test]))"

    :good ; refer with square brackets
    (->opts)
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer [is test]]))"

    :bad ; refer with parentheses
    (->opts)
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer (is test)]))"

    :bad ; refer unsorted
    (->opts)
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer [test is]]))"

    :good ; sorted requires
    (->opts)
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.data.xml]
   [clojure.test]))"

    :bad ; unsorted requires
    (->opts)
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test]
   [clojure.data.xml]))"

    :good ; sorted imports
    (->opts)
    "(ns thomas
  \"docstring\"
  (:import
   (java.io InputStream)
   (java.util Random)))"

    :bad ; unsorted imports
    (->opts)
    "(ns thomas
  \"docstring\"
  (:import
   (java.util Random)
   (java.io InputStream)))"

    :good ; sorted exclude
    (->opts)
    "(ns thomas
  \"docstring\"
  (:refer-clojure :exclude [and or]))"

    :bad ; unsorted exclude
    (->opts)
    "(ns thomas
  \"docstring\"
  (:refer-clojure :exclude [or and]))"

    :bad ; as/refer out of order
    (->opts)
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer [is] :as test]))"

    ;;
    ;; ns metadata support
    ;;
    :good
    (->opts)
    "(ns ^:bar ^:foo thomas
  \"docstring\")"

    :good
    (->opts)
    "(ns ^{:bar 12, :foo 18} thomas
  \"docstring\")"

    ;;
    ;; :gen-class support
    ;;
    :good
    (->opts)
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test])
  (:gen-class))"

    ;;
    ;; :require-macros support
    ;;
    :good
    (->opts)
    "(ns thomas
  \"docstring\"
  (:require-macros
   [clojure.test]))"
    ))
