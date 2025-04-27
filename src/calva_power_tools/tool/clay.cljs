(ns calva-power-tools.tool.clay
  (:require
   [calva-power-tools.extension.calva :as calva]))

(defn format-snippet
  ([clay-fn-name] (format-snippet clay-fn-name nil))
  ([clay-fn-name form]
   (str "(do (clojure.core/require '[scicloj.clay.v2.snippets])" \newline
        "  (scicloj.clay.v2.snippets/" clay-fn-name form "$file" "{:ide :calva}))" \newline)))

(defn activate! []
  ;; Register commands that call Calva's custom REPL command
  (calva/register-snippet! "clay.file" (format-snippet "make-ns-html!"))
  (calva/register-snippet! "clay.fileQuarto" (format-snippet "make-ns-quarto-html!"))
  (calva/register-snippet! "clay.fileRevealJs" (format-snippet "make-ns-quarto-revealjs!"))
  (calva/register-snippet! "clay.currentForm" (format-snippet "make-form-html!" "$current-form"))
  (calva/register-snippet! "clay.currentFormQuarto" (format-snippet "make-form-quarto-html!" "$current-form"))
  (calva/register-snippet! "clay.topLevelForm" (format-snippet "make-form-html!" "$top-level-form"))
  (calva/register-snippet! "clay.topLevelFormQuarto" (format-snippet "make-form-quarto-html!" "$top-level-form"))
  (calva/register-snippet! "clay.browse" (format-snippet "browse!"))
  (calva/register-snippet! "clay.watch" (format-snippet "watch!")))
