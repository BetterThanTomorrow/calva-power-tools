(ns calva-power-tools.extension.calva
  (:require ["vscode" :as vscode]
            [promesa.core :as p]))

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
