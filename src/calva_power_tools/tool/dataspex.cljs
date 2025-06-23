(ns calva-power-tools.tool.dataspex
  (:require
   ["vscode" :as vscode]
   [calva-power-tools.calva :as calva]
   [calva-power-tools.extension.db :as db]
   [calva-power-tools.extension.life-cycle-helpers :as lc-helpers]
   [calva-power-tools.tool.dataspex.panel-view :as panel-view]
   [calva-power-tools.util :as util]
   [clojure.string :as string]
   [promesa.core :as p]
   [calva-power-tools.extension.when-contexts :as when-contexts]))

(defn- get-label-candidate [form]
  (if (re-find #"\{|\(|\[" form)
    "CPT inspect"
    form))

(defn- check-dataspex-loaded []
  (-> (calva/evaluateCode+ "clj" "(clojure.core/boolean (clojure.core/find-ns 'dataspex.core))" "user")
      (p/then (fn [result]
                (= "true" (.-result result))))))

(defn- load-dependency []
  (util/load-dependency {:deps/mvn-name "no.cjohansen/dataspex"}))

(defn- start-dataspex-server []
  (-> (calva/evaluateCode+ "clj"
                           "(let [s (dataspex.core/start-server! {:port 0})]
                             (-> s .getConnectors first .getLocalPort))"
                           "user")
      (p/then (fn [result]
                (let [port (parse-long (.-result result))]
                  (swap! db/!app-db assoc :dataspex/port port))))))

(defn- ensure-dataspex-loaded-and-running []
  (p/let [is-loaded (check-dataspex-loaded)
          port (:dataspex/port @db/!app-db)]
    (if is-loaded
      (if port
        (p/resolved port)
        (start-dataspex-server))
      (p/let [_ (load-dependency)
              _ (calva/evaluateCode+ js/undefined "(clojure.core/require 'dataspex.core)" "user")]
        (start-dataspex-server)))))

(defn- open-in-editor-webview []
  (p/let [port (ensure-dataspex-loaded-and-running)]
    (calva/execute-calva-command! "calva.runCustomREPLCommand"
                                  {:snippet (str "(clojure.core/tagged-literal 'flare/html {:url \"http://localhost:" port "/\" :title \"Dataspex\"})")})))

(defn- open-in-panel-webview [!app-state context]
  (p/let [port (ensure-dataspex-loaded-and-running)]
    (when-contexts/set-context!+ !app-state :calva-power-tools/dataspex-panel-active? true)
    (panel-view/activate! context port)))

(defn- inspect-form [form label-candidate]
  (p/let [_ (ensure-dataspex-loaded-and-running)]
    (-> (vscode/window.showInputBox #js {:title "Dataspex Inspect: Panel item name"
                                         :ignoreFocusOut true
                                         :value label-candidate
                                         :placeHolder label-candidate})
        (.then (fn [s]
                 (calva/evaluateCode+ js/undefined (str "(dataspex.core/inspect\""
                                                        (if (string/blank? s)
                                                          "CPT inspect"
                                                          s)
                                                        "\" " form " )")))))))

(defn- inspect-current-form []
  (let [form (second (calva/currentForm))
        label (get-label-candidate form)]
    (inspect-form form label)))

(defn- inspect-top-level-form []
  (let [form (second (calva/currentTopLevelForm))
        label (get-label-candidate form)]
    (inspect-form form label)))

(defn- register-command! [command f]
  (lc-helpers/register-command! db/!app-db command f))

(defn activate! [!app-state context]
  (register-command! "cpt.dataspex.loadDataspexDependency" #'load-dependency)
  (register-command! "cpt.dataspex.inspectCurrentForm" #'inspect-current-form)
  (register-command! "cpt.dataspex.inspectTopLevelForm" #'inspect-top-level-form)
  (register-command! "cpt.dataspex.openInspectorInEditorView" #'open-in-editor-webview)
  (register-command! "cpt.dataspex.openInspectorPanelView" (partial #'open-in-panel-webview !app-state context)))
