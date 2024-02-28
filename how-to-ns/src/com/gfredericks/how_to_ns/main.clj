(ns com.gfredericks.how-to-ns.main
  (:require
   [clojure.java.io           :as io]
   [clojure.string            :as str]
   [com.gfredericks.how-to-ns :as how-to-ns])
  (:import
   [difflib DiffUtils Delta$TYPE]
   (java.io File)))

(set! *warn-on-reflection* true)

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

(defn ^:private all-clojure-files
  [paths]
  (println "paths => " (mapcat #(file-seq (File. ^String %)) paths))
  (->> paths
       (mapcat #(file-seq (File. ^String %)))
       (filter #(.isFile ^File %))
       (filter #(re-matches #".*\.clj[sc]?" (.getName ^File %)))))

(defn ^:private report-file-specific-exception
  [file ^Exception e]
  (binding [*out* *err*]
    (if (and (instance? IllegalArgumentException e)
             (= "Unreadable ns string!" (.getMessage e)))
      (do
        (printf "ERROR: Unreadable ns form in %s!\n  %s\n"
                (str file)
                ;; shouldn't be possible for the cause
                ;; to be nil, but just in case...
                (some-> e .getCause .getMessage pr-str))
        (flush))
      (do
        (println "Exception in how-to-ns when checking" (str file))
        (prn e)))))

(defn check
  [paths opts]
  (->> (all-clojure-files paths)
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
                  (report-file-specific-exception file e)
                  1))))
       (reduce +)))

(defn fix
  [paths opts]
  (doseq [file (all-clojure-files paths)]
    (try
      (let [contents (slurp file)
            formatted (how-to-ns/format-initial-ns-str contents opts)]
        (when (not= contents formatted)
          (println "Fixing" (str file))
          (spit file formatted)))
      (catch Exception e
        (report-file-specific-exception file e)
        ;; not sure whether it's better for fix to throw exceptions or
        ;; report problems in the return value (because under normal
        ;; conditions users won't need to look at the return value);
        ;; in any case this is more conservative to start with. It can
        ;; be revisited when writing -main.
        (throw e)))))

;; TODO: write a useful -main
