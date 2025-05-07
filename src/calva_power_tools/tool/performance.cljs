(ns calva-power-tools.tool.performance
  (:require
   ["vscode" :as vscode]
   [calva-power-tools.calva :as calva]
   [calva-power-tools.extension.db :as db]
   [calva-power-tools.extension.life-cycle-helpers :as lc-helpers]
   [calva-power-tools.util :as util]
   [promesa.core :as p]))

;; Dependencies loading functions

(defn- load-decompiler-dependency []
  (-> (util/load-dependency {:deps/mvn-name "com.clojure-goes-fast/clj-java-decompiler"
                             :deps/mvn-version "0.3.6"})
      (.then (fn [_]
               (calva/execute-calva-command!
                "calva.runCustomREPLCommand"
                #js {:snippet "(require '[clj-java-decompiler.core :refer [decompile disassemble]])"
                     :repl "clj"})))))

(defn- load-criterium-dependency []
  (-> (util/load-dependency {:deps/mvn-name "criterium/criterium"
                             :deps/mvn-version "0.4.6"})
      (.then (fn [_]
               (calva/execute-calva-command!
                "calva.runCustomREPLCommand"
                #js {:snippet "(require '[criterium.core :refer [quick-bench bench]])"
                     :repl "clj"})))))
;; Decompilation functions

(defn- decompile-top-level-form []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(require '[clj-java-decompiler.core :refer [decompile]]) (spit \"decompiled-$top-level-defined-symbol.java\" (with-out-str (decompile $top-level-form)))"
        :repl "clj"}))

(defn- decompile-top-level-form-unchecked-math []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(require '[clj-java-decompiler.core :refer [decompile]]) (spit \"decompiled-$top-level-defined-symbol.java\" (with-out-str (decompile (do (set! *unchecked-math* :warn-on-boxed) $top-level-form))))"
        :repl "clj"}))

(defn- decompile-selection []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(require '[clj-java-decompiler.core :refer [decompile]]) (spit \"decompiled-$top-level-defined-symbol.java\" (with-out-str (decompile (do $selection))))"
        :repl "clj"}))

(defn- disassemble-top-level-form []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(require '[clj-java-decompiler.core :refer [disassemble]]) (spit \"bytecode-$top-level-defined-symbol.class\" (with-out-str (disassemble $top-level-form)))"
        :repl "clj"}))

;; Benchmarking functions

(defn- quick-bench-top-level-form []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(require '[criterium.core :refer [quick-bench]]) (def ARG (read-line)) (quick-bench $top-level-form)"
        :repl "clj"}))

(defn- quick-bench-current-form []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(require '[criterium.core :refer [quick-bench]]) (quick-bench $current-form)"
        :repl "clj"}))

;; Time measurement functions

(defn- time-top-level-form []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(time $top-level-form)"
        :repl "clj"}))

(defn- time-current-form []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(time $current-form)"
        :repl "clj"}))

;; Profiler functions

(def profile-check-code (str '(try
                                (let [runtime (java.lang.management.ManagementFactory/getRuntimeMXBean)
                                      jvm-args (.getInputArguments runtime)
                                      required-opts #{"-Djdk.attach.allowAttachSelf"
                                                      "-XX:+UnlockDiagnosticVMOptions"
                                                      "-XX:+DebugNonSafepoints"}
                                      missing-opts (remove #(some (fn [arg] (.contains arg %)) jvm-args) required-opts)]
                                  (empty? missing-opts))
                                (catch Exception _
                                  false))))

(defn- load-profiler-dependency []
  (p/let [java-opts ":jvm-opts [\"-Djdk.attach.allowAttachSelf\" \"-XX:+UnlockDiagnosticVMOptions\" \"-XX:+DebugNonSafepoints\"]"
          evaluation (util/evaluateCode+ "clj" profile-check-code "user")
          attachable? (= "true" (.-result evaluation))]
    (if attachable?
      (-> (util/load-dependency {:deps/mvn-name "com.clojure-goes-fast/clj-async-profiler"
                                 :deps/mvn-version "1.5.1"})
          (.then (fn [_]
                   (calva/execute-calva-command!
                    "calva.runCustomREPLCommand"
                    #js {:snippet "(require '[clj-async-profiler.core :as prof])"
                         :repl "clj"}))))
      (-> (vscode/window.showInformationMessage (str "The REPL isn't started with Java optuons allowing the profiler to attach. "
                                                     "If you are using deps.edn, you can add a `:profiler` alias with: `" java-opts "`")
                                                "Copy options")
          (p/then (fn [button]
                    (when (= "Copy options" button)
                      (vscode/env.clipboard.writeText java-opts)
                      (vscode/window.showInformationMessage "Options copied to the clipboard"))))))))

(defn- profile-current-form []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(require '[clj-async-profiler.core :as prof]) (prof/profile $current-form)"
        :repl "clj"}))

(defn- profile-top-level-form []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(require '[clj-async-profiler.core :as prof]) (prof/profile $top-level-form)"
        :repl "clj"}))

(defn- start-profiler-ui []
  (let [auto-open (-> (vscode/workspace.getConfiguration "calva-power-tools")
                      (.get "performance.autoOpenProfilerUI"))]
    (p/let [evaluation (util/evaluateCode+ "clj" "(require '[clj-async-profiler.core :as prof]) (prof/serve-ui 9898)" "user")
            url (some->> (.-output evaluation)
                         (re-find #"Started server at /(.*?)\n?$")
                         second)]
      (case auto-open
        "vscode" (vscode/commands.executeCommand "simpleBrowser.show" (str "http://" url "/"))
        "system" (vscode/env.openExternal (vscode/Uri.parse (str "http://" url "/")))
        nil))))

(comment
  (re-find #"Started server at /(.*?)\n?$" "[clj-async-profiler.ui] Started server at /127.0.0.1:54586\n")
  :rcf)

(defn- share-to-flamebin []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "((requiring-resolve 'clj-async-profiler.flamebin/upload-to-flamebin) (read-line) {})"
        :repl "clj"}))

(defn- share-to-public-flamebin []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "((requiring-resolve 'clj-async-profiler.flamebin/upload-to-flamebin) (read-line) {:public? true})"
        :repl "clj"}))

;; Helper function
(defn- register-command! [command f]
  (lc-helpers/register-command! db/!app-db command f))

(defn activate! []
  ;; Register dependency loading commands

  ;; Register decompilation commands
  (register-command! "performance.loadDecompilerDependency" #'load-decompiler-dependency)
  (register-command! "performance.decompileTopLevelForm" #'decompile-top-level-form)
  (register-command! "performance.decompileTopLevelFormWithUncheckedMath" #'decompile-top-level-form-unchecked-math)
  (register-command! "performance.decompileSelection" #'decompile-selection)
  (register-command! "performance.disassembleTopLevelForm" #'disassemble-top-level-form)

  ;; Register benchmarking commands
  (register-command! "performance.loadCriteriumDependency" #'load-criterium-dependency)
  (register-command! "performance.quickBenchTopLevelForm" #'quick-bench-top-level-form)
  (register-command! "performance.quickBenchCurrentForm" #'quick-bench-current-form)

  ;; Register time measurement commands
  (register-command! "performance.timeTopLevelForm" #'time-top-level-form)
  (register-command! "performance.timeCurrentForm" #'time-current-form)

  ;; Register profiler commands
  (register-command! "performance.loadProfilerDependency" #'load-profiler-dependency)
  (register-command! "performance.profileCurrentForm" #'profile-current-form)
  (register-command! "performance.profileTopLevelForm" #'profile-top-level-form)
  (register-command! "performance.startProfilerUI" #'start-profiler-ui)
  (register-command! "performance.shareToFlamebin" #'share-to-flamebin)
  (register-command! "performance.shareToPublicFlamebin" #'share-to-public-flamebin))(ns calva-power-tools.tool.performance)