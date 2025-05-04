(ns calva-power-tools.tool.clay
  (:require
   [calva-power-tools.extension.calva :as calva]
   [clojure.string :as str]))

(defn make-snippet
  [clay-fn-name & args]
  {:snippet (str "(do (clojure.core/require '[scicloj.clay.v2.snippets])" \newline
                 "  (scicloj.clay.v2.snippets/" clay-fn-name (when args " ") (str/join " " args) "))" \newline)
   :repl "clj"})

(def file "\"$file\"")
(def current-form "(quote $current-form)")
(def top-level-form "(quote $top-level-form)")
(def options "{:ide :calva}")

(defn activate! []
  ;; Register commands that call Calva's custom REPL command
  (calva/register-snippet! "clay.makeFile" (make-snippet "make-ns-html!" file options))
  (calva/register-snippet! "clay.makeFileQuarto" (make-snippet "make-ns-quarto-html!" file options))
  (calva/register-snippet! "clay.makeFileRevealJs" (make-snippet "make-ns-quarto-revealjs!" file options))
  (calva/register-snippet! "clay.makeCurrentForm" (make-snippet "make-form-html!" current-form file options))
  (calva/register-snippet! "clay.makeCurrentFormQuarto" (make-snippet "make-form-quarto-html!" current-form file options))
  (calva/register-snippet! "clay.makeTopLevelForm" (make-snippet "make-form-html!" top-level-form file options))
  (calva/register-snippet! "clay.makeTopLevelFormQuarto" (make-snippet "make-form-quarto-html!" top-level-form file options))
  (calva/register-snippet! "clay.browse" (make-snippet "browse!"))
  (calva/register-snippet! "clay.watch" (make-snippet "watch!" options)))
