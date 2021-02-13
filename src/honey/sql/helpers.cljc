;; copyright (c) 2020-2021 sean corfield, all rights reserved

(ns honey.sql.helpers
  "Helper functions for the built-in clauses in honey.sql."
  (:refer-clojure :exclude [update set group-by for partition-by])
  (:require [honey.sql :as h]))

(defn- default-merge [current args]
  (into (vec current) args))

(defn- and-merge
  [current args]
  (let [args (remove nil? args)]
    (cond (= :and (first current))
          (default-merge current args)
          (seq current)
          (if (seq args)
            (default-merge [:and current] args)
            current)
          (= 1 (count args))
          (vec (first args))
          (seq args)
          (default-merge [:and] args)
          :else
          (vec current))))

(def ^:private special-merges
  {:where  #'and-merge
   :having #'and-merge})

(defn- helper-merge [data k args]
  (let [merge-fn (special-merges k default-merge)]
    (clojure.core/update data k merge-fn args)))

(defn- generic [k args]
  (if (map? (first args))
    (let [[data & args] args]
      (helper-merge data k args))
    (helper-merge {} k args)))

(defn- generic-1 [k [data arg]]
  (if arg
    (assoc data k arg)
    (assoc {} k data)))

(defn alter-table [& args] (generic :alter-table args))
(defn add-column [& args] (generic :add-column args))
(defn drop-column [& args] (generic-1 :drop-column args))
(defn modify-column [& args] (generic :modify-column args))
(defn rename-column [& args] (generic :rename-column args))
(defn add-index [& args] (generic :add-index args))
(defn drop-index [& args] (generic-1 :drop-index args))
(defn rename-table [& args] (generic :alter-table args))
(defn create-table [& args] (generic :create-table args))
(defn with-columns [& args]
  ;; special case so (with-columns [[:col-1 :definition] [:col-2 :definition]])
  ;; also works in addition to (with-columns [:col-1 :definition] [:col-2 :definition])
  (cond (and (= 1 (count args)) (sequential? (first args)) (sequential? (ffirst args)))
        (generic-1 :with-columns args)
        (and (= 2 (count args)) (sequential? (second args)) (sequential? (fnext args)))
        (generic-1 :with-columns args)
        :else
        (generic :with-columns args)))
(defn create-view [& args] (generic-1 :create-view args))
(defn drop-table [& args] (generic :drop-table args))
(defn nest [& args] (generic :nest args))
(defn with [& args] (generic :with args))
(defn with-recursive [& args] (generic :with-recursive args))
;; these five need to supply an empty hash map since they wrap
;; all of their arguments:
(defn intersect [& args] (generic :intersect (cons {} args)))
(defn union [& args] (generic :union (cons {} args)))
(defn union-all [& args] (generic :union-all (cons {} args)))
(defn except [& args] (generic :except (cons {} args)))
(defn except-all [& args] (generic :except-all (cons {} args)))

(defn select [& args] (generic :select args))
(defn select-distinct [& args] (generic :select-distinct args))
(defn insert-into [& args] (generic :insert-into args))
(defn update [& args] (generic :update args))
(defn delete [& args] (generic-1 :delete args))
(defn delete-from [& args] (generic :delete-from args))
(defn truncate [& args] (generic :truncate args))
(defn columns [& args] (generic :columns args))
(defn set [& args] (generic-1 :set args))
(defn from [& args] (generic :from args))
(defn using [& args] (generic :using args))
(defn join [& args] (generic :join args))
(defn left-join [& args] (generic :left-join args))
(defn right-join [& args] (generic :right-join args))
(defn inner-join [& args] (generic :inner-join args))
(defn outer-join [& args] (generic :outer-join args))
(defn full-join [& args] (generic :full-join args))
(defn cross-join [& args] (generic :cross-join args))
(defn where [& args] (generic :where args))
(defn group-by [& args] (generic :group-by args))
(defn having [& args] (generic :having args))
(defn window [& args] (generic :window args))
(defn partition-by [& args] (generic :partition-by args))
(defn order-by [& args] (generic :order-by args))
(defn limit [& args] (generic-1 :limit args))
(defn offset [& args] (generic-1 :offset args))
(defn for [& args] (generic-1 :for args))
(defn values [& args] (generic-1 :values args))
(defn on-conflict [& args] (generic-1 :on-conflict args))
(defn on-constraint [& args] (generic :on-constraint args))
(defn do-nothing [& args] (generic :do-nothing args))
(defn do-update-set [& args] (generic-1 :do-update-set args))
(defn returning [& args] (generic :returning args))

;; helpers that produce non-clause expressions -- must be listed below:
(defn composite [& args] (into [:composite] args))
;; to make this easy to use in a select, wrap it so it becomes a function:
(defn over [& args] [(into [:over] args)])

;; helper to ease compatibility with former nilenso/honeysql-postgres code:
(defn upsert [data & clauses] (default-merge data clauses))

#?(:clj
    (assert (= (clojure.core/set (conj @@#'h/base-clause-order
                                       :composite :over :upsert))
               (clojure.core/set (map keyword (keys (ns-publics *ns*)))))))
