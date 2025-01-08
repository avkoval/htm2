(ns tasks
  (:require [com.biffweb.tasks :as tasks]))

(defn hello
  "Says 'Hello'"
  []
  (println "Hello"))

;; Tasks should be vars (#'hello instead of hello) so that `clj -M:dev help` can
;; print their docstrings.

(defn css [_] nil)

(def custom-tasks
  {"hello" #'hello
   "css" #'css})

(def tasks (merge tasks/tasks custom-tasks))
