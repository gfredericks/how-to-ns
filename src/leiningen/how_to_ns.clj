(ns leiningen.how-to-ns
  "Lint clojure ns forms."
  (:require [leiningen.cljfmt :as cljfmt]
            [leiningen.cljfmt.diff :as diff]
            [leiningen.core.main :as main]))

(def default-opts
  {:require-docstring? true
   :sort-clauses? true
   :allow-refer-all? false
   :allow-extra-clauses? false
   :align-clauses? false
   :import-square-brackets? false})

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
     :require-macros (first (things :require-macros))
     :import (first (things :import))
     :gen-class (first (things :gen-class))
     :extra (concat (->> (select-keys things [:refer-clojure :require :require-macros :import])
                         (vals)
                         (mapcat rest))
                    (->> (dissoc things :refer-clojure :require :require-macros :import)
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

(defn join-symbols
  [& parts]
  (->> parts
       (clojure.string/join \.)
       (symbol)))

(defn split-symbol
  [sym]
  (let [parts (clojure.string/split (str sym) #"\.")]
    [(some->> parts
              (butlast)
              (seq)
              (clojure.string/join \.)
              (symbol))
     (symbol (last parts))]))

(defn update-when
  [m k f & args]
  (if (contains? m k)
    (apply update m k f args)
    m))

(defn normalize-require
  "Returns a collection of clauses."
  [require-clause opts]
  (let [require-clause (if (symbol? require-clause)
                         [require-clause]
                         require-clause)
        clauses (if (or (coll? (second require-clause))
                        (symbol? (second require-clause)))
                  (for [x (rest require-clause)]
                    (if (symbol? x)
                      [(join-symbols (first require-clause) x)]
                      (vec (cons (join-symbols (first require-clause) (first x))
                                 (rest x)))))
                  [require-clause])]
    (for [clause clauses
          :let [[ns-sym & kw-args] clause
                map-args (-> (apply hash-map kw-args)
                             (dissoc :rename)
                             (cond-> (not (:allow-refer-all? opts))
                               (update-when :refer #(if (= :all %) '[???] %)))
                             (update-when :refer
                                          (fn [arg]
                                            (if (coll? arg)
                                              (vec (sort arg))
                                              arg))))]]
      (apply vector ns-sym
             (apply concat (sort map-args))))))

(defn normalize-imports
  [import-clauses opts]
  (let [all-classes (mapcat (fn [import-clause]
                              (if (symbol? import-clause)
                                [import-clause]
                                (for [sym (rest import-clause)]
                                  (symbol (str (first import-clause) \. sym)))))
                            import-clauses)]
    (->> all-classes
         (map split-symbol)
         (group-by first)
         (sort)
         (map (fn [[package sym-pairs]]
                (apply (if (:import-square-brackets? opts)
                         vector
                         list)
                       package
                       (map second sym-pairs)))))))

(defn normalize-refer-clojure
  [refer-clojure-expr]
  (let [args (apply hash-map (rest refer-clojure-expr))]
    (cons :refer-clojure
          (-> args
              (update-when :exclude (comp vec sort))
              (update-when :only (comp vec sort))
              (->> (apply concat))))))

(defn print-ns-form
  [ns-form opts]
  (let [{:keys [ns doc refer-clojure require require-macros import gen-class extra]}
        (parse-ns-form ns-form)
        doc (or doc (if (:require-docstring? opts) "Perfunctory docstring."))
        ns-meta (meta ns)
        ns-meta-str (if ns-meta
                      (if (every? true? (vals ns-meta))
                        (->> (keys ns-meta)
                             (sort)
                             (map #(str \^ % \space))
                             (apply str))
                        (str "^{"
                             (->> ns-meta
                                  sort
                                  (map #(str (pr-str (key %)) " " (pr-str (val %))))
                                  (clojure.string/join ", "))
                             "} ")
                        )
                      "")]
    (printf "(ns %s%s" ns-meta-str ns)
    (when doc
      (print "\n  ")
      (print-string-with-line-breaks doc))
    (when refer-clojure
      (print "\n  ")
      (pr (normalize-refer-clojure refer-clojure)))
    (doseq [[name expr normalize-fn]
            [["require" require (fn [clauses]
                                  (mapcat #(normalize-require % opts)
                                          clauses))]
             ["require-macros" require-macros (fn [clauses]
                                                (mapcat #(normalize-require % opts)
                                                        clauses))]
             ["import" import #(normalize-imports % opts)]]

            :when expr
            :let [clauses (normalize-fn (rest expr))]
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
                               ;; make a symbol with whitespace just
                               ;; because it's cute
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
    (when gen-class
      (print "\n  ")
      (pr gen-class))
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

(defn exit
  [status]
  (when-not (zero? status)
    (main/exit status)))

(defn check
  [project files]
  (let [opts (merge default-opts (:how-to-ns project))]
    (->> files
         (map (fn [file]
                (let [relative-path (cljfmt/project-path project file)]
                  (try
                    (let [ns-str (slurp-ns-from-string (slurp file))
                          formatted (reformat-ns-str ns-str opts)]
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
         (exit))))

(defn fix
  [project files]
  (let [opts (merge default-opts (:how-to-ns project))]
    (doseq [file files
            :let [file-contents (slurp file)
                  ns-str (slurp-ns-from-string file-contents)
                  formatted (reformat-ns-str ns-str opts)]
            :when (not= ns-str formatted)]
      (println "Fixing" (cljfmt/project-path project file))
      (spit file (str formatted (subs file-contents (count ns-str)))))))

(defn all-files
  [project]
  ;; TODO: paste from lein-cljfmt?
  (->> (cljfmt/format-paths project)
       (mapcat #(cljfmt/find-files project %))))

(def usage
  "USAGE: lein how-to-ns <check|fix>")

(defn how-to-ns
  "Lint clojure ns forms.

USAGE:

  lein how-to-ns check # prints ns formatting problems
  lein how-to-ns fix   # corrects ns formatting problems"
  [project & args]
  (let [all-files (all-files project)]
    (case (first args)
      "check" (check project all-files)
      "fix"   (fix project all-files)
      (do
        (println usage)
        (main/exit 1)))))
