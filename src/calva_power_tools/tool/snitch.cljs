(ns calva-power-tools.tool.snitch
  (:require
   ["vscode" :as vscode]
   [calva-power-tools.calva :as calva]
   [calva-power-tools.extension.db :as db]
   [calva-power-tools.extension.life-cycle-helpers :as lc-helpers]
   [calva-power-tools.util :as util]
   [promesa.core :as p]
   [clojure.string :as string]))


(defn- instrument-form [form]
  (let [snitched (.replace form
                           #"\b(defn-?|defmethod|fn|let)(?=\s)"
                           (fn [s]
                             (case (string/trim s)
                               "defn-"     "defn*"
                               "defn"      "defn*"
                               "defmethod" "defmethod*"
                               "let"       "*let"
                               "fn"        "*fn"
                               s)))]
    (calva/execute-calva-command! "calva.runCustomREPLCommand"
                                  #js {:snippet snitched})))

(defn- instrument-top-level-form []
  (instrument-form (some-> (calva/currentTopLevelForm)
                           second)))

(defn- instrument-current-form []
  (instrument-form (some-> (calva/currentForm)
                           second)))

(defn- get-snitched-defn-results []
  (calva/execute-calva-command! "calva.runCustomREPLCommand"
                                #js {:snippet "${top-level-defined-symbol|replace|$|<}"}))

(defn- reconstruct-last-defn-call-to-clipboard []
  (-> (calva/execute-calva-command! "calva.runCustomREPLCommand"
                                    #js {:snippet "${top-level-defined-symbol|replace|$|>}"})
      (.then (fn [_]
               (p/let [ns (calva/getNamespace)
                       evaluation+ (calva/evaluateCode+ js/undefined "*1" ns)
                       last-call (.-result evaluation+)]
                 (vscode/env.clipboard.writeText last-call)
                 (vscode/window.showInformationMessage "The snitched call to this function is saved to the clipboard."))))))

(defn- load-dependency []
  (-> (util/load-dependency {:deps/mvn-name "org.clojars.abhinav/snitch"})
      (.then (fn [_]
               (calva/execute-calva-command!
                "calva.runCustomREPLCommand"
                #js {:snippet "(clojure.core/require '[snitch.core :refer [defn* defmethod* *fn *let]])"
                     :repl "clj"})))))

(defn- register-command! [command f]
  (lc-helpers/register-command! db/!app-db command f))

(defn activate! []
  ;; Register commands that call Calva's custom REPL command
  (register-command! "cpt.snitch.loadSnitchDependency" #'load-dependency)
  (register-command! "cpt.snitch.instrumentTopLevelForm" #'instrument-top-level-form)
  (register-command! "cpt.snitch.instrumentCurrentForm" #'instrument-current-form)
  (register-command! "cpt.snitch.getSnitchedDefnResults" #'get-snitched-defn-results)
  (register-command! "cpt.snitch.reconstructLastDefnCallToClipboard" #'reconstruct-last-defn-call-to-clipboard))
