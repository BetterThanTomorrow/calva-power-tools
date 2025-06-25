(ns calva-power-tools.tool.dataspex
  (:require
   ["vscode" :as vscode]
   [calva-power-tools.calva :as calva]
   [calva-power-tools.extension.db :as db]
   [calva-power-tools.extension.life-cycle-helpers :as lc-helpers]
   [calva-power-tools.tool.dataspex.panel-view :as panel-view]
   [calva-power-tools.util :as util]
   [clojure.edn :as edn]
   [clojure.string :as string]
   [promesa.core :as p]))

(defn- get-label-candidate [form]
  (if (re-find #"\{|\(|\[" form)
    "CPT inspect"
    form))

(defn- check-dataspex-loaded []
  (-> (calva/evaluateCode+ "clj" "(clojure.core/boolean (clojure.core/find-ns 'dataspex.core))" "user")
      (p/then (fn [result]
                (= "true" (.-result result))))))

(defn- load-dependency! []
  (util/load-dependency {:deps/mvn-name "no.cjohansen/dataspex"}))

(defn- maybe-start-server! []
  (-> (calva/evaluateCode+ "clj"
                           (str
                            '(if-let [get-server-info (clojure.core/requiring-resolve 'dataspex.core/get-server-info)]
                               (if (get-server-info)
                                 (assoc (get-server-info) :running? true)
                                 ((requiring-resolve 'dataspex.core/start-server!) {:port 0}))
                               {:error :get-server-info-missing}))
                           "user")
      (p/then (fn [result]
                (let [{:keys [port running? error]} (edn/read-string (.-result result))]
                  (if-not error
                    (do
                      (when-not running?
                        (println "Dataspex server started on port:" port))
                      port)
                    (throw (ex-info "Can't query Dataspex server-info. Dataspex 2025.06.8 or newer required." error))))))))

(defn- ensure-dataspex-loaded-and-running! []
  (p/let [is-loaded (check-dataspex-loaded)]
    (if is-loaded
      (maybe-start-server!)
      (p/let [choice (vscode/window.showInformationMessage "The Dataspex dependency is not loaded in the REPL. Do you want to load it?" "Yes" "No")]
        (when (= "Yes" choice)
          (p/let [_ (load-dependency!)
                  _ (calva/evaluateCode+ js/undefined "(#?(:clj clojure.core/require :cljs require) 'dataspex.core)")]
            (maybe-start-server!)))))))

(defn- open-in-editor-webview []
  (p/let [port (ensure-dataspex-loaded-and-running!)]
    (when port
      (calva/execute-calva-command! "calva.runCustomREPLCommand"
                                    {:snippet (str "(clojure.core/tagged-literal 'flare/html {:url \"http://localhost:" port "/\" :title \"Dataspex\"})")}))))

(defn- open-panel-webview! [!app-state context]
  (p/let [port (ensure-dataspex-loaded-and-running!)]
    (when port
      (panel-view/activate! !app-state context port))))

(defn- inspect-form [form label-candidate]
  (p/let [port (ensure-dataspex-loaded-and-running!)]
    (when port
      (-> (vscode/window.showInputBox #js {:title "Dataspex Inspect: Panel item name"
                                           :ignoreFocusOut true
                                           :value label-candidate
                                           :placeHolder label-candidate})
          (.then (fn [s]
                   (calva/evaluateCode+ js/undefined
                                        (str "(#?(:clj clojure.core/require :cljs require) 'dataspex.core)"
                                             "(dataspex.core/inspect\""
                                             (if (string/blank? s)
                                               "CPT inspect"
                                               s)
                                             "\" " form " )"))))))))

(defn- inspect-current-form []
  (let [form (second (calva/currentForm))
        label (get-label-candidate form)]
    (inspect-form form label)))

(defn- inspect-top-level-form []
  (let [form (second (calva/currentTopLevelForm))
        label (get-label-candidate form)]
    (inspect-form form label)))

(defn- connect-remote-inspector [!app-state context]
  (p/let [port (ensure-dataspex-loaded-and-running!)]
    (when port
      (p/let [eval-result (calva/evaluateCode+ js/undefined
                                               (str "(#?(:clj clojure.core/require :cljs require) 'dataspex.core)"
                                                    "(if-let [connect-remote-inspector (resolve 'dataspex.core/connect-remote-inspector)]"
                                                    "  (do (connect-remote-inspector \"http://localhost:" port "\")"
                                                    "      true)"
                                                    "  false)"))
              success? (clojure.edn/read-string (.-result eval-result))]
        (if success?
          (open-panel-webview! !app-state context)
          (throw (ex-info "Failed to connect the remote Dataspex inspector. You need to do this from a connected ClojureScript REPL."
                          {:port port})))))))

(defn- inspect-remote []
  (p/let [port (ensure-dataspex-loaded-and-running!)]
    (when port
      (p/let [eval-result (calva/evaluateCode+ js/undefined
                                               (str "(#?(:clj clojure.core/require :cljs require) 'dataspex.core)"
                                                    "(if-let [inspect-remote (resolve 'dataspex.core/inspect-remote)]"
                                                    "  (do (inspect-remote \"http://localhost:" port "\")"
                                                    "      true)"
                                                    "  false)"))
              success? (clojure.edn/read-string (.-result eval-result))]
        (when-not success?
          (throw (ex-info "Failed inspect to the remote Dataspex inspector."
                          {:port port})))))))

(defn- register-command! [command f]
  (lc-helpers/register-command! db/!app-db command f))

(defn activate! [!app-state context]
  (register-command! "cpt.dataspex.loadDataspexDependency" #'load-dependency!)
  (register-command! "cpt.dataspex.inspectCurrentForm" #'inspect-current-form)
  (register-command! "cpt.dataspex.inspectTopLevelForm" #'inspect-top-level-form)
  (register-command! "cpt.dataspex.openInspectorInEditorView" #'open-in-editor-webview)
  (register-command! "cpt.dataspex.openInspectorPanelView" (partial #'open-panel-webview! !app-state context))
  (register-command! "cpt.dataspex.connectRemoteInspector" (partial #'connect-remote-inspector  !app-state context))
  (register-command! "cpt.dataspex.inspectRemote" #'inspect-remote))
