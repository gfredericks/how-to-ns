(ns leiningen.how-to-ns
  "Lint clojure ns forms."
  (:require [com.gfredericks.how-to-ns :as core]
            [leiningen.core.main :as main]))

(defn how-to-ns
  "Lint clojure ns forms.

USAGE:

  lein how-to-ns check # prints ns formatting problems
  lein how-to-ns fix   # corrects ns formatting problems"
  [project & [command]]
  (let [all-files (core/all-files project)]
    (case command
      "check" (let [problem-count (core/check project all-files)]
                (when (pos? problem-count)
                  (main/exit 1)))
      "fix"   (core/fix project all-files)
      (do
        (println core/usage)
        (main/exit 1)))))
