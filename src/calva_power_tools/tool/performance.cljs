(ns calva-power-tools.tool.performance
  (:require
   ["vscode" :as vscode]
   [calva-power-tools.calva :as calva]
   [calva-power-tools.extension.db :as db]
   [calva-power-tools.extension.life-cycle-helpers :as lc-helpers]
   [calva-power-tools.util :as util]
   [promesa.core :as p]
   [clojure.string :as string]
   [clojure.edn :as edn]))

;; Decompilation functions

(defn- load-decompiler-dependency []
  (-> (util/load-dependency {:deps/mvn-name "com.clojure-goes-fast/clj-java-decompiler"
                             :deps/mvn-version "0.3.6"})
      (.then (fn [_]
               (calva/execute-calva-command!
                "calva.runCustomREPLCommand"
                #js {:snippet "(clojure.core/require '[clj-java-decompiler.core :refer [decompile disassemble]])"
                     :repl "clj"})))))

(defn- execute-and-open-untitled
  "Evaluate code in the REPL and open the result in an untitled document."
  [code title]
  (let [editor vscode/window.activeTextEditor
        document (some-> editor .-document)
        ns (some-> document calva/getNamespace)]
    (p/let [^js evaluation (calva/evaluateCode+ "clj" code ns)]
      (if (.-error evaluation)
        (vscode/window.showErrorMessage (str (.-error evaluation)
                                             " - "
                                             (.-errorOutput evaluation)))
        (p/let [content (.-result evaluation)
                untitled-doc (vscode/workspace.openTextDocument #js {:content (str "// " title "\n"
                                                                                   (edn/read-string content))
                                                                     :language "java"})]
          (vscode/window.showTextDocument untitled-doc #js {:viewColumn vscode/ViewColumn.Beside
                                                            :preserveFocus true}))))))

(defn- decompile-top-level-form []
  (let [function-name (some-> (calva/currentTopLevelDef)
                              second)
        file-name (str "decompiled-" function-name ".java")
        top-level-form (some-> (calva/currentTopLevelForm)
                               second)
        code (str "(clojure.core/require '[clj-java-decompiler.core :refer [decompile]]) "
                  "(clojure.core/with-out-str (decompile " (pr-str top-level-form) "))")]
    (execute-and-open-untitled code file-name)))

(defn- decompile-top-level-form-unchecked-math []
  (let [function-name (some-> (calva/currentTopLevelDef)
                              second)
        file-name (str "decompiled-" function-name "-unchecked.java")
        top-level-form (some-> (calva/currentTopLevelForm)
                               second)
        code (str "(clojure.core/require '[clj-java-decompiler.core :refer [decompile]]) "
                  "(clojure.core/with-out-str "
                  "  (decompile (do (set! *unchecked-math* :warn-on-boxed) " (pr-str top-level-form) ")))")]
    (execute-and-open-untitled code file-name)))

(defn- decompile-selection []
  (let [editor vscode/window.activeTextEditor
        selection (some-> editor .-selection)
        document (some-> editor .-document)
        selected-text (some-> document (.getText selection))
        function-name (some-> (calva/currentTopLevelDef)
                              second)
        file-name (str "decompiled-" function-name "-selection.java")
        code (str "(clojure.core/require '[clj-java-decompiler.core :refer [decompile]]) "
                  "(clojure.core/with-out-str "
                  "  (decompile (do " selected-text ")))")]
    (execute-and-open-untitled code file-name)))

(defn- disassemble-top-level-form []
  (let [function-name (some-> (calva/currentTopLevelDef)
                              second)
        file-name (str "bytecode-" function-name ".class")
        top-level-form (some-> (calva/currentTopLevelForm)
                               second)
        code (str "(clojure.core/require '[clj-java-decompiler.core :refer [disassemble]]) "
                  "(clojure.core/with-out-str (disassemble " (pr-str top-level-form) "))")]
    (execute-and-open-untitled code file-name)))

;; Benchmarking functions

(defn- load-criterium-dependency []
  (-> (util/load-dependency {:deps/mvn-name "criterium/criterium"
                             :deps/mvn-version "0.4.6"})
      (.then (fn [_]
               (calva/execute-calva-command!
                "calva.runCustomREPLCommand"
                #js {:snippet "(clojure.core/require '[criterium.core :refer [quick-bench bench]])"
                     :repl "clj"})))))

(defn- quick-bench-top-level-form []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(clojure.core/require '[criterium.core :refer [quick-bench]]) (criterium.core/quick-bench $top-level-form)"
        :repl "clj"}))

(defn- quick-bench-current-form []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(clojure.core/require '[criterium.core :refer [quick-bench]]) (criterium.core/quick-bench $current-form)"
        :repl "clj"}))

;; Time measurement functions

(defn- time-top-level-form []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(clojure.core/time $top-level-form)"
        :repl "clj"}))

(defn- time-current-form []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(clojure.core/time $current-form)"
        :repl "clj"}))

;; Profiler functions

(def profile-check-code (str '(try
                                (let [runtime (java.lang.management.ManagementFactory/getRuntimeMXBean)
                                      jvm-args (.getInputArguments runtime)
                                      required-opts #{"-Djdk.attach.allowAttachSelf"
                                                      "-XX:+UnlockDiagnosticVMOptions"
                                                      "-XX:+DebugNonSafepoints"}
                                      missing-opts (clojure.core/remove #(clojure.core/some (fn [arg] (.contains arg %)) jvm-args) required-opts)]
                                  (clojure.core/empty? missing-opts))
                                (catch Exception _
                                  false))))

(defn- with-profiler-check [f]
  (p/let [java-opts ":jvm-opts [\"-Djdk.attach.allowAttachSelf\" \"-XX:+UnlockDiagnosticVMOptions\" \"-XX:+DebugNonSafepoints\"]"
          evaluation (calva/evaluateCode+ "clj" profile-check-code "user")
          attachable? (= "true" (.-result evaluation))]
    (if attachable?
      (f)
      (-> (vscode/window.showInformationMessage
           (str "The REPL isn't started with Java options allowing the profiler to attach. "
                "If you are using deps.edn, you can add a `:profiler` alias with: `" java-opts "`")
           "Copy options")
          (p/then (fn [button]
                    (when (= "Copy options" button)
                      (vscode/env.clipboard.writeText java-opts)
                      (vscode/window.showInformationMessage "Options copied to the clipboard"))))))))

(defn- load-profiler-dependency []
  (with-profiler-check
    (fn []
      (-> (util/load-dependency {:deps/mvn-name "com.clojure-goes-fast/clj-async-profiler"
                                 :deps/mvn-version "1.6.1"})
          (.then (fn [_]
                   (calva/execute-calva-command!
                    "calva.runCustomREPLCommand"
                    #js {:snippet "(clojure.core/require '[clj-async-profiler.core :as prof])"
                         :repl "clj"})))))))

(defn- profile-current-form []
  (with-profiler-check
    (fn []
      (calva/execute-calva-command!
       "calva.runCustomREPLCommand"
       #js {:snippet "(clojure.core/require '[clj-async-profiler.core :as prof]) (prof/profile $current-form)"
            :repl "clj"}))))

(defn- profile-top-level-form []
  (with-profiler-check
    (fn []
      (calva/execute-calva-command!
       "calva.runCustomREPLCommand"
       #js {:snippet "(clojure.core/require '[clj-async-profiler.core :as prof]) (prof/profile $top-level-form)"
            :repl "clj"}))))

(defn- start-profiler-ui []
  (with-profiler-check
    (fn []
      (let [auto-open (-> (vscode/workspace.getConfiguration "cpt")
                          (.get "performance.autoOpenProfilerUI"))]
        (p/let [evaluation (calva/evaluateCode+ "clj" "(clojure.core/require '[clj-async-profiler.core :as prof]) (prof/serve-ui 0)" "user")
                url (some->> (.-output evaluation)
                             (re-find #"Started server at /(.*?)\n?$")
                             second)]
          (when (and url
                     (not (string/blank? url)))
            (case auto-open
              "vscode" (vscode/commands.executeCommand "simpleBrowser.show" (str "http://" url "/"))
              "system" (vscode/env.openExternal (vscode/Uri.parse (str "http://" url "/")))
              nil)))))))

;; Helper function
(defn- register-command! [command f]
  (lc-helpers/register-command! db/!app-db command f))

(defn activate! []
  ;; Register dependency loading commands

  ;; Register decompilation commands
  (register-command! "cpt.performance.loadDecompilerDependency" #'load-decompiler-dependency)
  (register-command! "cpt.performance.decompileTopLevelForm" #'decompile-top-level-form)
  (register-command! "cpt.performance.decompileTopLevelFormWithUncheckedMath" #'decompile-top-level-form-unchecked-math)
  (register-command! "cpt.performance.decompileSelection" #'decompile-selection)
  (register-command! "cpt.performance.disassembleTopLevelForm" #'disassemble-top-level-form)

  ;; Register benchmarking commands
  (register-command! "cpt.performance.loadCriteriumDependency" #'load-criterium-dependency)
  (register-command! "cpt.performance.quickBenchTopLevelForm" #'quick-bench-top-level-form)
  (register-command! "cpt.performance.quickBenchCurrentForm" #'quick-bench-current-form)

  ;; Register time measurement commands
  (register-command! "cpt.performance.timeTopLevelForm" #'time-top-level-form)
  (register-command! "cpt.performance.timeCurrentForm" #'time-current-form)

  ;; Register profiler commands
  (register-command! "cpt.performance.loadProfilerDependency" #'load-profiler-dependency)
  (register-command! "cpt.performance.profileCurrentForm" #'profile-current-form)
  (register-command! "cpt.performance.profileTopLevelForm" #'profile-top-level-form)
  (register-command! "cpt.performance.startProfilerUI" #'start-profiler-ui))