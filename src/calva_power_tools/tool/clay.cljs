(ns calva-power-tools.tool.clay
  (:require
   [calva-power-tools.extension.calva :as calva]
   [clojure.string :as str]))

(defn format-snippet
  [clay-fn-name & args]
  (str "(do (clojure.core/require '[scicloj.clay.v2.snippets])" \newline
       "  (scicloj.clay.v2.snippets/" clay-fn-name (when args " ") (str/join " " args) "))" \newline))

(def file "\"$file\"")
(def current-form "(quote $current-form)")
(def top-level-form "(quote $top-level-form)")
(def options "{:ide :calva}")

(defn activate! []
  ;; Register commands that call Calva's custom REPL command
  (calva/register-snippet! "clay.file" (format-snippet "make-ns-html!" file options))
  (calva/register-snippet! "clay.fileQuarto" (format-snippet "make-ns-quarto-html!" file options))
  (calva/register-snippet! "clay.fileRevealJs" (format-snippet "make-ns-quarto-revealjs!" file options))
  (calva/register-snippet! "clay.currentForm" (format-snippet "make-form-html!" current-form file options))
  (calva/register-snippet! "clay.currentFormQuarto" (format-snippet "make-form-quarto-html!" current-form file options))
  (calva/register-snippet! "clay.topLevelForm" (format-snippet "make-form-html!" top-level-form file options))
  (calva/register-snippet! "clay.topLevelFormQuarto" (format-snippet "make-form-quarto-html!" top-level-form file options))
  (calva/register-snippet! "clay.browse" (format-snippet "browse!"))
  (calva/register-snippet! "clay.watch" (format-snippet "watch!" options)))
