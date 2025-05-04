(ns calva-power-tools.extension
  (:require
   [calva-power-tools.extension.db :as db]
   [calva-power-tools.extension.life-cycle-helpers :as lc-helpers]
   [calva-power-tools.extension.when-contexts :as when-contexts]
   [calva-power-tools.tool.clay :as clay]
   [calva-power-tools.tool.tools-deps :as deps]))

;;;;; Extension activation entry point

(defn ^:export activate [context]
  (js/console.time "activation")
  (js/console.timeLog "activation" "Calva Power Tools activate START")

  (when context
    (swap! db/!app-db assoc :extension/context context))

  (clay/activate!)
  (deps/activate!)

  (when-contexts/set-context!+ db/!app-db :calva-power-tools/active? true)

  (js/console.timeLog "activation" "Calva Power Tools activate END")
  (js/console.timeEnd "activation")
  #js {:v1 {}})

(comment
  ;; When you have updated the activate function, cleanup and call activate again
  ;; NB: If you have updated the extension manifest, you will need to restart the extension host instead
  (lc-helpers/cleanup! db/!app-db)
  (activate nil)
  :rcf)

;;;;; Extension deactivation entry point

(defn ^:export deactivate []
  (lc-helpers/cleanup! db/!app-db))


;;;;; shadow-cljs hot reload hooks
;; We don't need to do anything here, but it is nice to see that reloading is happening

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ^{:dev/before-load true} before-load []
  (println "shadow-cljs reloading..."))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ^{:dev/after-load true} after-load []
  (println "shadow-cljs reload complete"))
