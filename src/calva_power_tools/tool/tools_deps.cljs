(ns calva-power-tools.tool.tools-deps
  (:require
   ["vscode" :as vscode]
   [calva-power-tools.extension.calva :as calva]
   [calva-power-tools.extension.db :as db]
   [calva-power-tools.extension.life-cycle-helpers :as lc-helpers]))


(defn activate! []
  (calva/register-snippet! "deps.loadSelectedDependencies"
                           {:snippet "(require '[clojure.repl.deps :refer [add-libs]]) (add-libs '{$selection})"
                            :ns "user"
                            :repl "clj"})
  (lc-helpers/register-command!
   db/!app-db "deps.loadDependencies"
   (fn []
     (-> (vscode/window.showInputBox #js {:title "Enter one or more deps.edn dependency coordinates"
                                          :ignoreFocusOut true
                                          :placeHolder "dev.weavejester/medley {:mvn/version \"1.8.1\"}"})
         (.then (fn [s]
                  (when s
                    (let [snippet {:snippet (str "(require '[clojure.repl.deps :refer [add-libs]]) (add-libs '{" s "})")
                                   :ns "user"
                                   :repl "clj"}]
                      (calva/execute-calva-command! "calva.runCustomREPLCommand"
                                                    (clj->js snippet))))))))))