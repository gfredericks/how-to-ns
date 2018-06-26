(ns leiningen.how-to-ns
  "Lint clojure ns forms."
  (:require
   [com.gfredericks.how-to-ns.main :as how-to-ns-main]
   [leiningen.core.main            :as lein-main])
  (:import
   (java.io File)))

(def ^:private usage
  "USAGE:

  lein how-to-ns check # prints ns formatting problems
  lein how-to-ns fix   # corrects ns formatting problems")

(defn ^:private relative-path
  [^File dir ^File file]
  (-> (.toURI dir)
      (.relativize (.toURI file))
      (.getPath)))

(doto
    (defn how-to-ns
      [project & [command]]
      (let [root  (File. (:root project))
            paths (->> (concat (:source-paths project)
                               (:test-paths project))
                       (map #(relative-path root (File. %))))
            opts  (:how-to-ns project)]
        (case command
          "check" (let [problem-count (how-to-ns-main/check paths opts)]
                    (when (pos? problem-count)
                      (lein-main/exit 1)))
          "fix"   (how-to-ns-main/fix paths opts)
          (do
            (println usage)
            (lein-main/exit 1)))))
  (alter-meta! assoc :doc
               (str "Lint clojure ns forms.\n\n" usage)))
