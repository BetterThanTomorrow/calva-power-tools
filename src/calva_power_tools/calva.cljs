(ns calva-power-tools.calva
  (:require
   ["vscode" :as vscode]
   [calva-power-tools.extension.db :as db]
   [calva-power-tools.extension.life-cycle-helpers :as lc-helpers]
   [promesa.core :as p]))

(def ^:private ^js calva-ext (vscode/extensions.getExtension "betterthantomorrow.calva"))

(def ^:private calva-api (-> calva-ext
                             .-exports
                             .-v1
                             (js->clj :keywordize-keys true)))

(def evaluateCode+ (get-in calva-api [:repl :evaluateCode]))
(def currentFunction (get-in calva-api [:ranges :currentFunction]))
(def currentTopLevelDef (get-in calva-api [:ranges :currentTopLevelDef]))
(def currentTopLevelForm (get-in calva-api [:ranges :currentTopLevelForm]))
(def currentForm (get-in calva-api [:ranges :currentForm]))
(def getNamespace (get-in calva-api [:document :getNamespace]))

(defn execute-calva-command!
  "Safely executes a Calva command and shows user-friendly errors.
   Returns a promise resolving to true if the command likely succeeded, false otherwise."
  [command & args]
  (p/let [calva-ext (.getExtension vscode/extensions "betterthantomorrow.calva")]
    (cond
      (nil? calva-ext)
      (do
        (.showErrorMessage vscode/window "Calva is not installed. This feature requires Calva.")
        false)

      (not (.-isActive calva-ext))
      (do
        (.showErrorMessage vscode/window "Calva is not active. Please ensure it's enabled.")
        false)

      :else
      (let [js-args (clj->js (cons command args))]
        (-> (.apply (.-executeCommand vscode/commands) vscode/commands js-args)
            (p/then (fn [result] (some? result)))
            (p/catch (fn [error]
                       (.showErrorMessage vscode/window (str "Failed to run Calva command '" command "': " error))
                       false)))))))

(defn register-snippet! [command snippet]
  (lc-helpers/register-command! db/!app-db command
                                (fn []
                                  (execute-calva-command! "calva.runCustomREPLCommand"
                                                          (clj->js snippet)))))

