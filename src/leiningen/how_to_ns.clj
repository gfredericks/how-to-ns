(ns leiningen.how-to-ns
  (:require [leiningen.cljfmt :as cljfmt]
            [leiningen.cljfmt.diff :as diff]
            [leiningen.core.main :as main]))

(def default-opts
  {:require-docstring? false
   :sort-clauses? true
   :allow-refer-all? true
   :allow-extra-clauses? false
   :align-clauses? true})

(defn parse-ns-form
  [[_ns-sym ns-name-sym & more]]
  (let [[doc more] (if (string? (first more))
                     [(first more) (rest more)]
                     [nil more])
        things (group-by #(and (seq? %) (first %)) more)]
    {:ns ns-name-sym
     :doc doc
     :refer-clojure (first (things :refer-clojure))
     :require (first (things :require))
     :import (first (things :import))
     :extra (concat (->> (select-keys things [:refer-clojure :require :import])
                         (vals)
                         (mapcat rest))
                    (->> (dissoc things :refer-clojure :require :import)
                         (mapcat vals)))}))

(defn print-string-with-line-breaks
  [s]
  (print \")
  (let [lines (clojure.string/split s #"\n")]
    (->> lines
         (map pr-str)
         (map #(->> % rest butlast (apply str)))
         (clojure.string/join "\n")
         print)
    (print \")))

(defn align-left
  [str length]
  (format (format "%%-%ds" length) str))

(defn print-ns-form
  [ns-form opts]
  (let [{:keys [ns doc refer-clojure require import extra]}
        (parse-ns-form ns-form)
        doc (or doc (if (:require-docstring? opts) "Honorary docstring."))]
    (printf "(ns %s" ns)
    (when doc
      (print "\n  ")
      (print-string-with-line-breaks doc))
    (when refer-clojure
      (print "\n  ")
      (pr refer-clojure))
    (doseq [[name expr] [["require" require]
                         ["import" import]]
            :when expr
            :let [clauses (rest expr)]
            :when (seq clauses)]

      (printf "\n  (:%s\n" name)
      (let [name-field-length (->> clauses
                                   (map first)
                                   (map str)
                                   (map count)
                                   (apply max))
            clauses (cond->> clauses
                      (:sort-clauses? opts)
                      (sort-by #(if (coll? %) (first %) %))
                      (:align-clauses? opts)
                      (map (fn [clause]
                             (if (and (coll? clause)
                                      (symbol? (first clause))
                                      (seq (rest clause)))
                               ;; make a symbol with whitespace just for funsies
                               (let [new-sym (symbol (align-left (first clause)
                                                                 name-field-length))]
                                 (if (vector? clause)
                                   (assoc clause 0 new-sym)
                                   (cons new-sym (rest clause))))
                               clause))))]
        (doseq [clause (butlast clauses)]
          (print "   ")
          (prn clause))
        (print "   ")
        (pr (last clauses))
        (print ")")))
    (print \))))

(defn valid-ns-form?
  [ns-form opts]
  (and (= 'ns (first ns-form))
       (symbol? (second ns-form))
       (let [{:keys [ns doc refer-clojure require import extra]}
             (parse-ns-form ns-form)]
         (and (or (:allow-extra-clauses? opts)
                  (empty? extra))
              (or (not (:require-docstring? opts))
                   doc)))))

(defn slurp-ns-from-string
  [s]
  ;; cribbed from clojure.repl/source-fn
  (with-open [rdr (java.io.StringReader. s)]
    (let [text (StringBuilder.)
          pbr (proxy [java.io.PushbackReader] [rdr]
                (read [] (let [i (proxy-super read)]
                           (.append text (char i))
                           i)))]
      (if (= :unknown *read-eval*)
        (throw (IllegalStateException. "Unable to read source while *read-eval* is :unknown."))
        (read {} (java.io.PushbackReader. pbr)))
      (str text))))

(defn reformat-ns-str
  [ns-str opts]
  (with-out-str
    (print-ns-form (read-string ns-str) opts)))

(defn check
  [project files]
  (->> files
       (map (fn [file]
              (let [relative-path (cljfmt/project-path project file)]
                (try
                  (let [ns-str (slurp-ns-from-string (slurp file))
                        formatted (reformat-ns-str ns-str default-opts)]
                    (if (= ns-str formatted)
                      0
                      (do
                        (binding [*out* *err*]
                          (println "Bad ns format:")
                          (println (diff/unified-diff
                                    relative-path
                                    ns-str
                                    formatted)))
                        1)))
                  (catch Exception e
                    (binding [*out* *err*]
                      (println "Exception in how-to-ns when checking" relative-path)
                      (prn e))
                    1)))))
       (reduce +)
       (main/exit)))

(defn fix
  [files]
  (println 'what))

(defn all-files
  [project]
  ;; TODO: paste from lein-cljfmt?
  (->> (cljfmt/format-paths project)
       (mapcat #(cljfmt/find-files project %))))

(def help
  "HOW TO NS!")

(defn how-to-ns
  "I don't do a lot."
  [project & args]
  (let [all-files (all-files project)]
    (case (first args)
      "check" (check project all-files)
      "fix"   (fix project all-files)
      (println help))))


(comment

  (print-ns-form
   '(thomas
     "here's my docistr
  I can put whatever I want
  ini it"
     (:refer-clojure :exclude [what])
     (:require [clojure.core :as core]
               [thomas.whatsit]
               [and.this.is-longer-.than-that :refer [no]])
     (:import (java.util List)
              (java.util.concurrent TimeUnit)))
   default-opts)
  )
