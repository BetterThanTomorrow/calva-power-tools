(ns calva-power-tools.tool.clay
  (:require
   [calva-power-tools.calva :as calva]
   [calva-power-tools.extension.db :as db]
   [calva-power-tools.extension.life-cycle-helpers :as lc-helpers]
   [calva-power-tools.util :as util]
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
  (calva/register-snippet! "cpt.clay.makeFile" (make-snippet "make-ns-html!" file options))
  (calva/register-snippet! "cpt.clay.makeFileQuarto" (make-snippet "make-ns-quarto-html!" file options))
  (calva/register-snippet! "cpt.clay.makeFileRevealJs" (make-snippet "make-ns-quarto-revealjs!" file options))
  (calva/register-snippet! "cpt.clay.makeCurrentForm" (make-snippet "make-form-html!" current-form file options))
  (calva/register-snippet! "cpt.clay.makeCurrentFormQuarto" (make-snippet "make-form-quarto-html!" current-form file options))
  (calva/register-snippet! "cpt.clay.makeTopLevelForm" (make-snippet "make-form-html!" top-level-form file options))
  (calva/register-snippet! "cpt.clay.makeTopLevelFormQuarto" (make-snippet "make-form-quarto-html!" top-level-form file options))
  (calva/register-snippet! "cpt.clay.browse" (make-snippet "browse!"))
  (calva/register-snippet! "cpt.clay.watch" (make-snippet "watch!" options))

  (lc-helpers/register-command!
   db/!app-db "cpt.clay.loadClayDependency"
   (fn []
     (util/load-dependency {:deps/mvn-name "org.scicloj/clay"}))))
