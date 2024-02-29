(ns com.gfredericks.tool
  (:require
   [com.gfredericks.config :as config]
   [com.gfredericks.how-to-ns.main :as main]))

(def default-paths
  ["src" "test"])

(defn apply-check-or-fix-fn
  [check-or-fix-fn options]
  (let [paths (or (:paths options) default-paths)
        opts (merge (config/load-config) options)]
    (check-or-fix-fn paths opts)))

(defn check
  [options]
  (let [problem-count (apply-check-or-fix-fn main/check options)]
    (when (pos? problem-count)
      (System/exit 1))))

(defn fix
  [options]
  (apply-check-or-fix-fn main/fix options))

(defn deps
  [_]
  (println "dependencies loaded"))
