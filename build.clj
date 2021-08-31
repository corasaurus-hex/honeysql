(ns build
  "HoneySQL's build script.

  clojure -T:build run-tests
  clojure -T:build run-tests :aliases '[:master]'

  clojure -T:build ci

  For more information, run:

  clojure -A:deps -T:build help/doc"

  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))

(def lib 'com.github.seancorfield/honeysql)
(def version (format "2.0.%s" (b/git-count-revs nil)))

(defn eastwood "Run Eastwood." [opts]
  (-> opts (bb/run-task [:eastwood])))

(defn gen-doc-tests "Generate tests from doc code blocks." [opts]
  (-> opts (bb/run-task [:gen-doc-tests])))

(defn run-doc-tests
  "Generate and run doc tests.

  Optionally specify :plaftorm:
  :1.9 -- test against Clojure 1.9 (the default)
  :1.10 -- test against Clojure 1.10.3
  :master -- test against Clojure 1.11 master snapshot
  :cljs -- test against ClojureScript"
  [{:keys [platform] :or {platform :1.9} :as opts}]
  (gen-doc-tests opts)
  (bb/run-tests (assoc opts :aliases
                       (conj [:test-doc platform]
                             (if (= :cljs platform)
                               :test-doc-cljs
                               :test-doc-clj)))))

(defn ci "Run the CI pipeline of tests (and build the JAR)." [opts]
  (-> opts
      (bb/clean)
      (assoc :lib lib :version version)
      (as-> opts
            (reduce (fn [opts platform]
                      (run-doc-tests (assoc opts :platform platform)))
                    opts
                    [:cljs :1.9 :1.10 :master]))
      (eastwood)
      (as-> opts
            (reduce (fn [opts alias]
                      (bb/run-tests (assoc opts :aliases [alias])))
                    opts
                    [:cljs :1.9 :1.10 :master]))
      (bb/clean)
      (bb/jar)))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/deploy)))
