(ns leiningen.how-to-ns)

(def default-opts
  {:require-docstring? false
   :sort-clauses? true
   :allow-refer-all? true
   :allow-extra-clauses? false
   :align-clauses? true})

(defn parse-ns-args
  [[sym & more]]
  (let [[doc more] (if (string? (first more))
                     [(first more) (rest more)]
                     [nil more])
        things (group-by #(and (seq? %) (first %)) more)]
    {:ns sym
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
  [ns-args opts]
  (let [{:keys [ns doc refer-clojure require import extra]}
        (parse-ns-args ns-args)]
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
                      (sort-by #(if (coll? %) (vec %) %))
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

(defn valid-ns-args?
  [ns-args opts]
  (and (symbol? (first ns-args))
       (let [{:keys [ns doc refer-clojure require import extra]}
             (parse-ns-args ns-args)]
         (and (or (:allow-extra-clauses? opts)
                  (empty? extra))
              (or (not (:require-docstring? opts))
                   doc)))))

(defn how-to-ns
  "I don't do a lot."
  [project & args]
  (println "Hi!"))


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
