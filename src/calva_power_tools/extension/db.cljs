(ns calva-power-tools.extension.db)

(def init-db {:extension/context nil
              :extension/disposables []
              :extension/when-contexts {:calva-power-tools/active? false}})

(defonce !app-db (atom init-db))

(comment
  @!app-db
  :rcf)

