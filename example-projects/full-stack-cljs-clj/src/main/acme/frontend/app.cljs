(ns acme.frontend.app)

(defonce !client-state (atom {}))

(comment
  (swap! !client-state assoc :app/hello :world)
  :rcf)

(defn ^:dev/after-load init []
  (println "Hello World")
  (-> js/document
      (.getElementById "root")
      (.-innerHTML)
      (set! "Acme App started.")))