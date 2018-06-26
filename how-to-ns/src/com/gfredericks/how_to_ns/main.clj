(ns com.gfredericks.how-to-ns.main
  (:require
   [clojure.java.io           :as io]
   [clojure.string            :as str]
   [com.gfredericks.how-to-ns :as how-to-ns])
  (:import
   [difflib DiffUtils Delta$TYPE]
   (java.io File)))

;; next three functions pasted from cljfmt
(defn ^:private lines
  [s]
  (str/split s #"\n"))

(defn ^:private unlines
  [ss]
  (str/join "\n" ss))

(defn ^:private unified-diff
  ([filename original revised]
   (unified-diff filename original revised 3))
  ([filename original revised context]
   (unlines (DiffUtils/generateUnifiedDiff
             ;; TODO: write something that will do something reasonabl
             ;; here if filename is an absolute path
             (str (io/file "a" filename))
             (str (io/file "b" filename))
             (lines original)
             (DiffUtils/diff (lines original) (lines revised))
             context))))

(defn ^:private all-files
  [paths]
  (->> paths
       (mapcat #(file-seq (File. %)))
       (filter #(.isFile %))))

(defn check
  [paths opts]
  (->> (all-files paths)
       (map (fn [file]
              (try
                (let [contents (slurp file)
                      formatted (how-to-ns/format-initial-ns-str contents opts)]
                  (if (= contents formatted)
                    0
                    (do
                      (binding [*out* *err*]
                        (println "Bad ns format:")
                        (println (unified-diff
                                  (str file)
                                  contents
                                  formatted)))
                      1)))
                (catch Exception e
                  (binding [*out* *err*]
                    (println "Exception in how-to-ns when checking" file)
                    (prn e))
                  1))))
       (reduce +)))

(defn fix
  [paths opts]
  (doseq [file (all-files paths)
          :let [contents (slurp file)
                formatted (how-to-ns/format-initial-ns-str contents opts)]
          :when (not= contents formatted)]
    (println "Fixing" (str file))
    (spit file formatted)))

;; TODO: write a useful -main
