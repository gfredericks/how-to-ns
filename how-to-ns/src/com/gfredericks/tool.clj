(ns com.gfredericks.tool
  (:require
   [com.gfredericks.how-to-ns :as how-to-ns]
   [com.gfredericks.how-to-ns.main :as main]))

(def default-paths
  ["src" "test"])

(defn apply-check-or-fix-fn
  [check-or-fix-fn options]
  (let [paths (or (:paths options) default-paths)
        opts (merge how-to-ns/default-opts options)]
    (check-or-fix-fn paths opts)))

(defn check
  [options]
  (let [problem-count (apply-check-or-fix-fn main/check options)]
    (when (pos? problem-count)
      (System/exit 1))))

(defn fix
  [options]
  (apply-check-or-fix-fn main/fix options))
