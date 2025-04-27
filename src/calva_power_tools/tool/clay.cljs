(ns calva-power-tools.tool.clay
  (:require
   [calva-power-tools.extension.calva :as calva]))

(defn activate! []
  ;; Register commands that call Calva's custom REPL command
  (calva/register-snippet! "clay.topLevelForm"
                           '(do (clojure.core/require '[scicloj.clay.v2.snippets])
                                (scicloj.clay.v2.snippets/make-form-html!
                                 (quote $top-level-form) "$file" {:ide :calva})))
  (calva/register-snippet! "clay.file"
                           '(do (clojure.core/require '[scicloj.clay.v2.snippets])
                                (scicloj.clay.v2.snippets/make-ns-html!
                                 "$file" {:ide :calva})))
  (calva/register-snippet! "clay.currentForm"
                           '(do (clojure.core/require '[scicloj.clay.v2.snippets])
                                (scicloj.clay.v2.snippets/make-form-html!
                                 (quote $current-form) "$file" {:ide :calva})))
  (calva/register-snippet! "clay.watch"
                           '(do (clojure.core/require '[scicloj.clay.v2.snippets])
                                (scicloj.clay.v2.snippets/watch! {:ide :calva}))))
