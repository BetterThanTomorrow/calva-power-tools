(ns calva-power-tools.util
  (:require
   ["vscode" :as vscode]))

(defn tool-dependency-load-version-snippet [{:config/keys [path]
                                             :deps/keys [mvn-name]}]
  (let [version (-> (vscode/workspace.getConfiguration "calva-power-tools")
                    (.get path))]
    {:snippet (str "((requiring-resolve 'clojure.repl.deps/add-libs)
                    {'" mvn-name " {:mvn/version \"" version "\"}})")
     :ns "user"
     :repl "clj"}))

(defn tool-dependency-load-snippet [{:deps/keys [mvn-name]}]
  {:snippet (str "((requiring-resolve 'clojure.repl.deps/add-lib)
                    '" mvn-name ")")
   :ns "user"
   :repl "clj"})