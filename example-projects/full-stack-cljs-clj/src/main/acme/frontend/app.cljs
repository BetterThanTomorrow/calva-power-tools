(ns acme.frontend.app)

(defonce !client-state (atom {:client/counter 0}))

(comment
  (swap! !client-state assoc :client/hello :world)
  :rcf)

(defn ^:export increment-counter! []
  (swap! !client-state update :client/counter inc))

(defn app-html [{:client/keys [counter]}]
  (str "<div style='text-align: center; padding: 2rem; font-family: Arial, sans-serif;'>"
       "<h1>Acme Counter App</h1>"
       "<div style='margin: 2rem;'>"
       "<h2>Counter: " counter "</h2>"
       "</div>"
       "<button onclick='acme.frontend.app.increment_counter_BANG_()' "
       "style='padding: 1rem 2rem; font-size: 1.2rem; background: #4CAF50; color: white; border: none; border-radius: 4px; cursor: pointer;'>"
       "Increment Counter"
       "</button>"
       "</div>"))

(defn ^:dev/after-load update-dom! []
  (-> js/document
      (.getElementById "root")
      (.-innerHTML)
      (set! (app-html @!client-state))))

(defn ^:export init! []
  (println "Hello World")
  (add-watch !client-state :dom-update
             (fn [_k _r _o _n] (update-dom!)))
  (update-dom!))