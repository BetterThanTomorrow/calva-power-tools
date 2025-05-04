(ns tests.extension-test
  (:require ["vscode" :as vscode]
            [cljs.test :refer [testing is]]
            [promesa.core :as p]
            [e2e.macros :refer [deftest-async]]))

(deftest-async registered-commands-exist
  (testing "Check if contributed commands are registered"
    (p/let [;; Get commands from the extension's manifest
            extension-id "betterthantomorrow.calva-power-tools"
            extension (vscode/extensions.getExtension extension-id)
            package-json (when extension (.-packageJSON extension))
            contributed-commands (when package-json
                                   (->> (.-contributes package-json)
                                        (.-commands)
                                        (map #(.-command %))
                                        (into #{}))) ; Convert to set
            ;; Get all currently registered commands
            all-commands (-> (vscode/commands.getCommands true)
                             (p/then js->clj))
            available-commands-set (set all-commands)

            ;; Find commands from manifest that are NOT registered
            missing-commands (when contributed-commands
                               (->> contributed-commands
                                    (filter #(not (contains? available-commands-set %)))
                                    (into #{})))]
      (is (some? extension)
          (str "The extension is found: " extension-id))
      (is (seq contributed-commands)
          (str "The manifest contributes commands " contributed-commands))
      (is (empty? missing-commands)
          "All commands from the manifest are registered"))))
