(ns calva-power-tools.tool.dataspex
  (:require
   ["vscode" :as vscode]
   [calva-power-tools.calva :as calva]
   [calva-power-tools.extension.db :as db]
   [calva-power-tools.extension.life-cycle-helpers :as lc-helpers]
   [calva-power-tools.tool.dataspex.panel-view :as panel-view]
   [calva-power-tools.util :as util]
   [clojure.string :as string]
   [calva-power-tools.extension.when-contexts :as when-contexts]))

(defn- get-label-candidate [form]
  (when-not (re-find #"\{|\(|\[" form)
    form))

(defn- load-dependency []
  (util/load-dependency {:deps/mvn-name "no.cjohansen/dataspex"}))

(defn- inspect-form [form label-candidate]
  (-> (vscode/window.showInputBox #js {:title "Dataspex Inspect: Panel item name"
                                       :ignoreFocusOut true
                                       :value label-candidate
                                       :placeHolder "Inspected thing"})
      (.then (fn [s]
               (let [snippet {:snippet (str "(require 'dataspex.core) (dataspex.core/inspect\""
                                            (if (string/blank? s)
                                              "Inspected thing"
                                              s)
                                            "\" " form " )")}]
                 (calva/execute-calva-command! "calva.runCustomREPLCommand"
                                               (clj->js snippet)))))))


(defn- inspect-current-form []
  (let [form (second (calva/currentForm))
        label (get-label-candidate form)]
    (inspect-form form label)))

(defn- inspect-top-level-form []
  (let [form (second (calva/currentTopLevelForm))
        label (get-label-candidate form)]
    (inspect-form form label)))

(tagged-literal 'flare/html {:url "http://localhost:7117/" :title "Dataspex"})

(defn- open-in-editor-webview []
  (calva/execute-calva-command! "calva.runCustomREPLCommand"
                                #js {:snippet "(tagged-literal 'flare/html {:url \"http://localhost:7117/\" :title \"Dataspex\"})"}))

(defn- open-in-panel-webview [!app-state context]
  (when-contexts/set-context!+ !app-state :calva-power-tools/dataspex-panel-active? true)
  (panel-view/activate! context))

(defn- register-command! [command f]
  (lc-helpers/register-command! db/!app-db command f))

(defn activate! [!app-state context]
  (register-command! "cpt.dataspex.loadDataspexDependency" #'load-dependency)
  (register-command! "cpt.dataspex.inspectCurrentForm" #'inspect-current-form)
  (register-command! "cpt.dataspex.inspectTopLevelForm" #'inspect-top-level-form)
  (register-command! "cpt.dataspex.openInspectorInEditorView" #'open-in-editor-webview)
  (register-command! "cpt.dataspex.openInspectorPanelView" (partial #'open-in-panel-webview !app-state context)))
