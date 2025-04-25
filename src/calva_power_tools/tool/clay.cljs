(ns calva-power-tools.tool.clay)

(def make-current-form-snippet
  '(do (require '[scicloj.clay.v2.snippets])
       (scicloj.clay.v2.snippets/make-form-html!
        (quote $current-form) "$file" {:ide :calva})))

(def make-toplevel-form-snippet
  '(do (require '[scicloj.clay.v2.snippets])
       (scicloj.clay.v2.snippets/make-form-html!
        (quote $top-level-form) "$file" {:ide :calva})))

(def make-file-snippet
  '(do (require '[scicloj.clay.v2.snippets])
       (scicloj.clay.v2.snippets/make-ns-html!
        "$file" {:ide :calva})))

(def watch-snippet
  '(do (require '[scicloj.clay.v2.snippets])
       (scicloj.clay.v2.snippets/watch! {:ide :calva})))

(defn command-args [snippet]
  (clj->js {:snippet (str snippet)
            :repl "clj"}))
