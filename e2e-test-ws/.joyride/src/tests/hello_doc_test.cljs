(ns tests.hello-doc-test
  (:require ["vscode" :as vscode]
            [cljs.test :refer [testing is]]
            [promesa.core :as p]
            [e2e.macros :refer [deftest-async]]))

(deftest-async registered-commands-exist
  (testing "Check if registered commands exist"
    (p/let [all-commands (-> (vscode/commands.getCommands true)
                             (p/then js->clj))
            registered-commands #{"clay.showTopLevelForm" "clay.makeFile" "clay.watch"}
            available-commands-set (set all-commands)
            missing-commands (->> registered-commands
                                  (filter #(not (contains? available-commands-set %)))
                                  (into #{}))]
      (is (empty? missing-commands) (str "Missing commands: " missing-commands)))))
