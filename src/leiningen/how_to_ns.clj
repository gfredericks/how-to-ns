(ns leiningen.how-to-ns)

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

(defn print-ns-form
  [ns-args]
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
            :let [clauses (sort (rest expr))]
            :when (seq clauses)]

      (printf "\n  (:%s\n" name)
      (let [name-field-length (->> clauses
                                   (map first)
                                   (map str)
                                   (map count)
                                   (apply max))]
        (doseq [clause (butlast clauses)]
          (print "   ")
          ;; TODO: align?
          (prn clause))
        (pr (last require))
        (println ")")))
    (print \))))

(defn valid-ns-args?
  [ns-args]
  (and (symbol? (first ns-args))
       (let [{:keys [ns doc refer-clojure require import extra]}
             (parse-ns-args ns-args)]
         (and (empty? extra)
              #_ doc ;; uncomment to require docstrings

              ))))

(defn how-to-ns
  "I don't do a lot."
  [project & args]
  (println "Hi!"))
