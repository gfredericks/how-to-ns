(ns com.gfredericks.how-to-ns
  "Lint clojure ns forms."
  (:import
   (java.io PushbackReader StringReader)))

(set! *warn-on-reflection* true)

(def default-opts
  {:require-docstring?               true
   :sort-clauses?                    true
   :allow-refer-all?                 false
   :allow-extra-clauses?             false
   :allow-rename?                    false
   :align-clauses?                   false
   :import-square-brackets?          false
   :sort-string-requires-to-end?     false})

(defn parse-ns-form
  [[_ns-sym ns-name-sym & more]]
  (let [[doc more] (if (string? (first more))
                     [(first more) (rest more)]
                     [nil more])
        things (group-by (fn [x]
                           (cond
                             (seq? x) (first x)
                             (reader-conditional? x) :reader-conditionals))
                         more)]
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
                         (mapcat vals)))
     :reader-conditionals (things :reader-conditionals)}))

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
  (if-not (clojure.string/includes? sym ".")
    [sym]
    (let [parts (clojure.string/split (str sym) #"\.")]
      [(some->> parts
                (butlast)
                (seq)
                (clojure.string/join \.)
                (symbol))
       (symbol (last parts))])))

(defn update-when
  [m k f & args]
  (if (contains? m k)
    (apply update m k f args)
    m))

(defn normalize-require
  "Returns a collection of clauses."
  [require-clause opts]
  (let [require-clause (if (or (symbol? require-clause)
                               (string? require-clause))
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
                             (cond-> (not (:allow-rename? opts))
                               (dissoc :rename))
                             (cond-> (not (:allow-refer-all? opts))
                               (update-when :refer #(if (= :all %) '[???] %)))
                             (update-when :refer
                                          (fn [arg]
                                            (if (coll? arg)
                                              (vec (sort arg))
                                              arg))))]]
      (apply vector ns-sym
             (apply concat (sort map-args))))))

(defn maybe-normalize-require [require-clause opts]
  (if (reader-conditional? require-clause)
    [require-clause]
    (normalize-require require-clause opts)))

(defn normalize-imports
  [import-clauses opts]
  (let [all-classes (mapcat (fn [import-clause]
                              (cond
                                (symbol? import-clause)
                                [import-clause]

                                (reader-conditional? import-clause)
                                [import-clause]

                                (-> import-clause rest seq)
                                (for [sym (rest import-clause)]
                                  (str (pr-str (first import-clause)) \. (pr-str sym)))

                                :else
                                [(first import-clause)]))
                            import-clauses)]
    (->> all-classes
         (map (fn [x]
                (if (reader-conditional? x)
                    [x]
                    (split-symbol x))))
         (group-by first)
         (sort-by (fn [[x]]
                    (pr-str x)))
         (map (fn [[package sym-pairs]]
                [package
                 (vec (sort-by (fn [[packge sym]]
                                 (print-str sym))
                               sym-pairs))]))
         (map (fn [[package sym-pairs]]
                (if (reader-conditional? package)
                  package
                  (apply (if (:import-square-brackets? opts)
                           vector
                           list)
                         package
                         (keep second sym-pairs))))))))

(defn normalize-refer-clojure
  [refer-clojure-expr]
  (let [args (apply hash-map (rest refer-clojure-expr))]
    (cons :refer-clojure
          (-> args
              (update-when :exclude (comp vec sort))
              (update-when :only (comp vec sort))
              (->> (apply concat))))))

(defn require-sort-criterion [{:keys [sort-string-requires-to-end?]}]
  (fn [x]
    (let [namespace (if (coll? x)
                      (first x)
                      x)
          criterion (str namespace)]
      (if sort-string-requires-to-end?
        [(if (string? namespace) 1 0) criterion]
        criterion))))

(defn print-ns-form
  [ns-form opts]
  (let [{:keys [ns doc refer-clojure require require-macros import reader-conditionals gen-class extra]}
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
                                  (mapcat #(maybe-normalize-require % opts)
                                          clauses))]
             ["require-macros" require-macros (fn [clauses]
                                                (mapcat #(maybe-normalize-require % opts)
                                                        clauses))]
             ["import" import #(normalize-imports % opts)]]

            :when expr
            :let [clauses (normalize-fn (rest expr))]
            :when (seq clauses)]

      (printf "\n  (:%s\n" name)
      (let [name-field-length (->> clauses
                                   (remove reader-conditional?)
                                   (map first)
                                   (map str)
                                   (map count)
                                   (apply max 0))
            clauses (cond->> clauses
                      (:sort-clauses? opts)
                      (sort-by (require-sort-criterion opts))
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
    (doseq [reader-conditional reader-conditionals]
      (print "\n  ")
      (pr reader-conditional))
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
  (with-open [rdr (StringReader. s)]
    (let [text (StringBuilder.)
          pbr (proxy [PushbackReader] [rdr]
                ;; the this this here is apparently the only
                ;; way to type-hint a proxy-super call to
                ;; avoid reflection
                (read [] (let [^PushbackReader this this
                               i (proxy-super read)]
                           (.append text (char i))
                           i)))]
      (if (= :unknown *read-eval*)
        (throw (IllegalStateException. "Unable to read source while *read-eval* is :unknown."))
        (try
          (read {:read-cond :preserve} (PushbackReader. pbr))
          (catch Exception e
            (throw (IllegalArgumentException. "Unreadable ns string!" e)))))
      (str text))))

(defn ^:deprecated reformat-ns-str
  "DEPRECATED: Use format-ns-str."
  [ns-str opts]
  (with-out-str
    (print-ns-form (read-string {:read-cond :preserve} ns-str) opts)))

(defn format-ns-str
  "Returns a formatted version of ns-str, according to opts."
  [ns-str opts]
  (let [opts (merge default-opts opts)]
    (reformat-ns-str ns-str opts)))

(defn good-ns-str?
  "Returns true if the given string matches the return value from
  format-ns-str."
  [ns-str opts]
  (= ns-str (format-ns-str ns-str opts)))

(defn starts-with-good-ns-str?
  "Returns true if the given string begins with an ns form for which
  good-ns-str? returns true."
  [file-str opts]
  (good-ns-str? (slurp-ns-from-string file-str) opts))

(defn format-initial-ns-str
  "Returns a variant of the given string with the initial ns form
  formatted according to format-ns-str."
  [file-str opts]
  (let [ns-str (slurp-ns-from-string file-str)
        ns-str' (format-ns-str ns-str opts)]
    (str ns-str' (subs file-str (count ns-str)))))
