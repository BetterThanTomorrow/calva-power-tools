(ns calva-power-tools.tool.performance
  (:require
   ["vscode" :as vscode]
   [calva-power-tools.calva :as calva]
   [calva-power-tools.extension.db :as db]
   [calva-power-tools.extension.life-cycle-helpers :as lc-helpers]
   [calva-power-tools.util :as util]
   [promesa.core :as p]
   [clojure.string :as string]))

;; Decompilation functions

(defn- load-decompiler-dependency []
  (-> (util/load-dependency {:deps/mvn-name "com.clojure-goes-fast/clj-java-decompiler"
                             :deps/mvn-version "0.3.6"})
      (.then (fn [_]
               (calva/execute-calva-command!
                "calva.runCustomREPLCommand"
                #js {:snippet "(clojure.core/require '[clj-java-decompiler.core :refer [decompile disassemble]])"
                     :repl "clj"})))))

(defn- decompile-top-level-form []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(clojure.core/require '[clj-java-decompiler.core :refer [decompile]]) (clojure.core/spit \"decompiled-$top-level-defined-symbol.java\" (clojure.core/with-out-str (decompile $top-level-form)))"
        :repl "clj"}))

(defn- decompile-top-level-form-unchecked-math []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(clojure.core/require '[clj-java-decompiler.core :refer [decompile]]) (clojure.core/spit \"decompiled-$top-level-defined-symbol.java\" (clojure.core/with-out-str (decompile (do (set! *unchecked-math* :warn-on-boxed) $top-level-form))))"
        :repl "clj"}))

(defn- decompile-selection []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(clojure.core/require '[clj-java-decompiler.core :refer [decompile]]) (clojure.core/spit \"decompiled-$top-level-defined-symbol.java\" (clojure.core/with-out-str (decompile (do $selection))))"
        :repl "clj"}))

(defn- disassemble-top-level-form []
  (calva/execute-calva-command!
   "calva.runCustomREPLCommand"
   #js {:snippet "(clojure.core/require '[clj-java-decompiler.core :refer [disassemble]]) (clojure.core/spit \"bytecode-$top-level-defined-symbol.class\" (clojure.core/with-out-str (disassemble $top-level-form)))"
        :repl "clj"}))

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
          evaluation (util/evaluateCode+ "clj" profile-check-code "user")
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
      (let [auto-open (-> (vscode/workspace.getConfiguration "calva-power-tools")
                          (.get "performance.autoOpenProfilerUI"))]
        (p/let [evaluation (util/evaluateCode+ "clj" "(clojure.core/require '[clj-async-profiler.core :as prof]) (prof/serve-ui 0)" "user")
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
  (register-command! "performance.startProfilerUI" #'start-profiler-ui))