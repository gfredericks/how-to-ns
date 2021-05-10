(ns com.gfredericks.how-to-ns-test
  (:require
   [clojure.test :refer :all]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.properties :as prop]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.how-to-ns :as how-to-ns]))

(def test-cases
  [;; stu's example
   {:outcome :good
    :opts {}
    :ns-str
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
   (org.apache.commons.io FileUtils)))"}

   ;; simplest
   {:outcome :good
    :opts {:require-docstring? false}
    :ns-str
    "(ns thomas)"}

   ;; with a required docstring
   {:outcome :good
    :opts {}
    :ns-str
    "(ns thomas
  \"here's my
  docstring\")"}

   ;; total garbage
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas thomas)"}

   ;; needs docstring
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas)"}

   ;; clauses in wrong order
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:import
   (java.util Random))
  (:require
   [clj-time.core]))"}

   ;; unparenthesized clauses
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas.disney
  \"docstring\"
  (:require
   clj-time.core))"}

   ;; import using square brackets
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas.disney
  \"docstring\"
  (:import
   [java.util Date]))"}

   ;; import using square brackets when allowed
   {:outcome :good
    :opts {:import-square-brackets? true}
    :ns-str
    "(ns thomas.disney
  \"docstring\"
  (:import
   [java.util Date]))"}

   ;; requires using parentheses
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:require
   (clojure.data.xml)))"}

   ;; :refer [not all]
   {:outcome :good
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer [not-all]]))"}

   ;; :refer :all
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer :all]))"}

   ;; :refer :all when allowed
   {:outcome :good
    :opts {:allow-refer-all? true}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer :all]))"}

   ;; :rename
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer [deftest] :rename {deftest tefdest}]))"}

   ;; :allow-rename?
   {:outcome :good
    :opts {:allow-rename? true}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer [deftest] :rename {deftest tefdest}]))"}

   ;; two requires
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test])
  (:require
   [clojure.data.xml]))"}

   ;; renames
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer [is] :rename {is isn't}]))"}

   ;; use
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:use [clojure.test]))"}

   ;; require instead of :require
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (require [clojure.test]))"}

   ;; refer with square brackets
   {:outcome :good
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer [is test]]))"}

   ;; refer with parentheses
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer (is test)]))"}

   ;; refer unsorted
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer [test is]]))"}

   ;; sorted requires
   {:outcome :good
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.data.xml]
   [clojure.test]))"}

   ;; unsorted requires
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test]
   [clojure.data.xml]))"}

   ;; sorted imports
   {:outcome :good
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:import
   (java.io File InputStream)
   (java.util Random)))"}

   ;; unsorted imports
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:import
   (java.util Random)
   (java.io InputStream)))"}

   ;; unsorted imports classes
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:import
   (java.io InputStream File)))"}

   ;; sorted exclude
   {:outcome :good
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:refer-clojure :exclude [and or]))"}

   ;; unsorted exclude
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:refer-clojure :exclude [or and]))"}

   ;; as/refer out of order
   {:outcome :bad
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test :refer [is] :as test]))"}

   ;;
   ;; ns metadata support
   ;;
   {:outcome :good
    :opts {}
    :ns-str
    "(ns ^:bar ^:foo thomas
  \"docstring\")"}

   {:outcome :good
    :opts {}
    :ns-str
    "(ns ^{:bar 12, :foo 18} thomas
  \"docstring\")"}

   ;;
   ;; :gen-class support
   ;;
   {:outcome :good
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:require
   [clojure.test])
  (:gen-class))"}

   ;;
   ;; :require-macros support
   ;;
   {:outcome :good
    :opts {}
    :ns-str
    "(ns thomas
  \"docstring\"
  (:require-macros
   [clojure.test]))"}])

(deftest require-sort-criterion
  (are [input option expected] (= expected
                                  (sort-by (how-to-ns/require-sort-criterion {:sort-string-requires-to-end? option})
                                           input))
    '(c "b" a) false '(a "b" c)
    '(c "b" a) true  '(a c "b")))

(deftest requires-processing
  (testing "`require` clasuses are processed and sorted. npm-style ones are also handled"
    (are [input expected] (= expected
                             (how-to-ns/format-ns-str input {:require-docstring? false}))
      "(ns foo)"
      "(ns foo)"

      "(ns foo (:require foo))"
      "(ns foo\n  (:require\n   [foo]))"

      "(ns foo (:require \"foo\"))"
      "(ns foo\n  (:require\n   [\"foo\"]))"

      "(ns foo (:require [\"foo\"]))"
      "(ns foo\n  (:require\n   [\"foo\"]))"

      "(ns foo (:require [\"foo\" :as bar]))"
      "(ns foo\n  (:require\n   [\"foo\" :as bar]))"

      "(ns foo (:require [\"foo\" :as bar] goofy abc))"
      "(ns foo\n  (:require\n   [abc]\n   [\"foo\" :as bar]\n   [goofy]))"))

  (testing "`:sort-string-requires-to-end?` option"
    (are [option input expected] (= expected
                                    (how-to-ns/format-ns-str input {:require-docstring? false
                                                                    :sort-string-requires-to-end? option}))

      false
      "(ns foo (:require [\"foo\" :as bar] goofy abc))"
      "(ns foo\n  (:require\n   [abc]\n   [\"foo\" :as bar]\n   [goofy]))"

      true
      "(ns foo (:require [\"foo\" :as bar] goofy abc))"
      "(ns foo\n  (:require\n   [abc]\n   [goofy]\n   [\"foo\" :as bar]))")))

(deftest it-works
  (doseq [{:keys [outcome opts ns-str]} test-cases]
    (is ((case outcome :good identity :bad not)
         (how-to-ns/good-ns-str? ns-str opts)))
    (is (how-to-ns/good-ns-str?
         (how-to-ns/format-ns-str ns-str opts)
         opts)
        "Accepts its own reformatting.")))

(deftest reformatting
  (are [input expected] (= expected
                           (how-to-ns/format-ns-str input {:require-docstring? false}))
    "(ns foo)"
    "(ns foo)"

    "(ns foo (:require foo))"
    "(ns foo\n  (:require\n   [foo]))"

    "(ns foo (:require #?(:clj foo :cljs bar)))"
    "(ns foo\n  (:require\n   #?(:clj foo :cljs bar)))"

    "(ns foo (:require [#?(:clj foo :cljs bar)]))"
    "(ns foo\n  (:require\n   [#?(:clj foo :cljs bar)]))"

    "(ns foo (:require #?(:clj foo :cljs bar)#?(:clj baz :cljs quux)))"
    "(ns foo\n  (:require\n   #?(:clj foo :cljs bar)\n   #?(:clj baz :cljs quux)))"

    "(ns foo (:require [#?(:clj foo :cljs bar)] THING))"
    "(ns foo\n  (:require\n   [THING]\n   [#?(:clj foo :cljs bar)]))"

    "(ns foo (:require #?(:clj foo :cljs bar) THING #?(:clj baz :cljs quux) OTHER))"
    "(ns foo\n  (:require\n   [OTHER]\n   [THING]\n   #?(:clj foo :cljs bar)\n   #?(:clj baz :cljs quux)))"

    "(ns foo (:import (A2 a b) (A1 b a)))"
    "(ns foo\n  (:import\n   (A1 a b)\n   (A2 a b)))"

    "(ns foo (:import #?(:clj foo :cljs bar)))"
    "(ns foo\n  (:import\n   #?(:clj foo :cljs bar)))"

    "(ns foo (:import #?(:clj foo :cljs bar)#?(:clj baz :cljs quux)))"
    "(ns foo\n  (:import\n   #?(:clj foo :cljs bar)\n   #?(:clj baz :cljs quux)))"

    "(ns foo (:import #?(:clj foo :cljs bar) (A2 a) #?(:clj baz :cljs quux) (A1 a)))"
    "(ns foo\n  (:import\n   (A1 a)\n   (A2 a)\n   #?(:clj foo :cljs bar)\n   #?(:clj baz :cljs quux)))"

    "(ns foo (:import (A)))"
    "(ns foo\n  (:import\n   (A)))"

    "(ns foo (:import (#?(:clj foo :cljs bar))))"
    "(ns foo\n  (:import\n   #?(:clj foo :cljs bar)))"

    "(ns foo (:import (#?(:clj foo :cljs bar) baz)))"
    "(ns foo\n  (:import\n   (#?(:clj foo :cljs bar) baz)))"

    "(ns foo (:import (Foo #?(:clj bar :cljs baz) d)))"
    "(ns foo\n  (:import\n   (Foo #?(:clj bar :cljs baz) d)))"
    
    "(ns foo #?(:clj (:import (A))))"
    "(ns foo\n  #?(:clj (:import (A))))"

    "(ns foo (:require [bar :refer [b a]]))"
    "(ns foo\n  (:require\n   [bar :refer [a b]]))"

    "(ns foo (:require [bar :refer-macros :all]))"
    "(ns foo\n  (:require\n   [bar :refer-macros [???]]))"

    "(ns foo (:require [bar :refer-macros [b a]]))"
    "(ns foo\n  (:require\n   [bar :refer-macros [a b]]))"

    "(ns foo (:require [bar #?(:clj :refer :cljs :refer-macros) [b a]]))"
    "(ns foo\n  (:require\n   [bar #?(:clj :refer :cljs :refer-macros) [a b]]))"

    "(ns foo (:require [bar #?(:cljs :refer-macros :clj :refer) [b a]]))"
    "(ns foo\n  (:require\n   [bar #?(:cljs :refer-macros :clj :refer) [a b]]))"

    "(ns foo (:require [bar :refer-macros [d c] :refer [b a]]))"
    "(ns foo\n  (:require\n   [bar :refer [a b] :refer-macros [c d]]))"))

(deftest traditional-newline-style
  (are [input expected] (= expected
                           (how-to-ns/format-ns-str input {:require-docstring? false
                                                           :traditional-newline-style? true}))
    "(ns foo (:require foo))"
    "(ns foo\n  (:require [foo]))"

    "(ns foo (:require foo bar))"
    "(ns foo\n  (:require [bar]\n            [foo]))"
    
    "(ns foo (:require foo bar baz))"
    "(ns foo\n  (:require [bar]\n            [baz]\n            [foo]))"

    "(ns foo (:require foo bar baz) (:import (java.io File) (java.util UUID)))"
    "(ns foo\n  (:require [bar]\n            [baz]\n            [foo])\n  (:import (java.io File)\n           (java.util UUID)))"))

(defspec judgment-unaffected-by-arbitrary-contents-following-ns-str 500
  (prop/for-all [{:keys [opts ns-str]} (gen/elements test-cases)
                 aftergarbage gen/string]
    (let [file-str (str ns-str aftergarbage)]
      (and (= (how-to-ns/good-ns-str? ns-str opts)
              (how-to-ns/starts-with-good-ns-str? file-str opts))
           (let [fixed (how-to-ns/format-initial-ns-str file-str opts)]
             (and (how-to-ns/starts-with-good-ns-str? fixed opts)
                  (.endsWith ^String fixed aftergarbage)))))))
