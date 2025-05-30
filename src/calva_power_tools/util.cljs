(ns calva-power-tools.util
  (:require
   ["vscode" :as vscode]
   [calva-power-tools.calva :as calva]
   [promesa.core :as p]))

(defn- code-for-dependency-loading [{:deps/keys [mvn-name]}]
  (str "(if-let [add-lib (resolve 'clojure.repl.deps/add-lib)]
          (do (clojure.core/println \"Adding dependency: \"'" mvn-name ")
              (add-lib '" mvn-name "))
          (throw (clojure.core/ex-info (clojure.core/str \"FAILED adding dependency '" mvn-name ". clojure.repl.deps/add-lib was not found, Clojure 1.12 or higher is required\") {})))"))

(defn load-dependency
  "Load a dependency using Calva's REPL API directly.
   Shows a progress dialog and handles errors with VS Code notifications.

   Options map:
     :deps/mvn-name - Maven artifact name (e.g. 'org.scicloj/clay')"
  [{:deps/keys [mvn-name] :as m}]
  (let [code (code-for-dependency-loading m)]
    (-> (.withProgress vscode/window
                       #js {:location vscode/ProgressLocation.Notification
                            :title (str "Loading dependency: " mvn-name)
                            :cancellable false}
                       (fn [_progress _token]
                         (p/create
                          (fn [resolve reject]
                            (-> (calva/evaluateCode+
                                 "clj" code "user"
                                 #js {:stdout (fn [output]
                                                (js/console.log (str "Dependency loading stdout: " output)))
                                      :stderr (fn [err]
                                                (js/console.error (str "Dependency loading error: " err))
                                                (when (not= err "")
                                                  (reject err)))})
                                (p/then (fn [result]
                                          (resolve (.-result result))))
                                (p/catch reject))))))
        (p/then (fn [evaluation+]
                  (vscode/window.showInformationMessage (str "Dependency " mvn-name " loaded.") "OK")
                  evaluation+))
        (p/catch (fn [error-message]
                   (vscode/window.showErrorMessage
                    (str "Failed to load dependency " mvn-name ": " error-message)
                    "OK")
                   nil)))))
