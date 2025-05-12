(ns calva-power-tools.tool.snitch
  (:require
   ["vscode" :as vscode]
   [calva-power-tools.calva :as calva]
   [calva-power-tools.extension.db :as db]
   [calva-power-tools.extension.life-cycle-helpers :as lc-helpers]
   [calva-power-tools.util :as util]
   [promesa.core :as p]
   [clojure.reader :as reader]
   [clojure.string :as string]))


(defn- instrument-form [form]
  (let [pattern (re-pattern "\\b(defn|fn|let)\\b")
        snitched (string/replace-first form pattern (fn [s]
                                                      (case s
                                                        "defn" "defn*"
                                                        "let" "*let"
                                                        "fn" "*fn")))]
    (calva/execute-calva-command! "calva.runCustomREPLCommand"
                                  #js {:snippet snitched})))

(defn- instrument-defn []
  (instrument-form (-> (util/currentTopLevelForm)
                       second)))

(defn- get-snitched-defn-results []
  (calva/execute-calva-command! "calva.runCustomREPLCommand"
                                #js {:snippet "${top-level-defined-symbol|replace|$|<}"}))

(defn- reconstruct-last-defn-call-to-clipboard []
  (-> (calva/execute-calva-command! "calva.runCustomREPLCommand"
                                    #js {:snippet "${top-level-defined-symbol|replace|$|>}"})
      (.then (fn [_]
               (p/let [ns (util/getNamespace)
                       evaluation+ (util/evaluateCode+ js/undefined "*1" ns)
                       last-call (.-result evaluation+)]
                 (vscode/env.clipboard.writeText last-call)
                 (vscode/window.showInformationMessage "The snitched call to this function is saved to the clipboard."))))))

(defn- load-dependency []
  (-> (util/load-dependency {:deps/mvn-name "org.clojars.abhinav/snitch"})
      (.then (fn [_]
               (calva/execute-calva-command!
                "calva.runCustomREPLCommand"
                #js {:snippet "(require '[snitch.core :refer [defn* defmethod* *fn *let]])"
                     :repl "clj"})))))

(defn- register-command! [command f]
  (lc-helpers/register-command! db/!app-db command f))

(defn activate! []
  ;; Register commands that call Calva's custom REPL command
  (register-command! "snitch.loadSnitchDependency" #'load-dependency)
  (register-command! "snitch.instrumentTopLevelForm" #'instrument-defn)
  (register-command! "snitch.getSnitchedDefnResults" #'get-snitched-defn-results)
  (register-command! "snitch.reconstructLastDefnCallToClipboard" #'reconstruct-last-defn-call-to-clipboard))
