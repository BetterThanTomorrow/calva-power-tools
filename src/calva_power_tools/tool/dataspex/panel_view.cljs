(ns calva-power-tools.tool.dataspex.panel-view
  (:require
   ["vscode" :as vscode]
   [calva-power-tools.extension.when-contexts :as when-contexts]))

(defn getWebViewHtml [port]
  (let [port-str (str port)]
    (str "<!DOCTYPE html>
    <html lang='en'>
    <head>
        <meta charset='UTF-8'>
        <meta name='viewport' content='width=device-width, initial-scale=1.0'>
        <title>Dataspex</title>
        <style>
            body, html, iframe {
                margin: 0;
                padding: 0;
                height: 100%;
                width: 100%;
                overflow: hidden;
            }
        </style>
    </head>
    <body>
        <iframe src='http://localhost:" port-str "/' frameborder='0'
                style='position: absolute; top: 0; left: 0; width: 100%; height: 100%;'
                allow='clipboard-read; clipboard-write'></iframe>
    </body>
    </html>")))

(defn- resolveWebviewView [^js this ^js webviewView _context _token]
  (println "resolveWebviewView")
  (let [webview (.-webview webviewView)
        port (.-_port this)]
    (unchecked-set webview "options" #js{:enableScripts true
                                         :localResourceRoots #js [(.-_extensionUri this)]})
    (unchecked-set webview "html" (getWebViewHtml port))
    (.onDidReceiveMessage webview (fn [data]
                                    (js/console.log "webview.onDidReceiveMessage" data)))
    (.onDidChangeVisibility webviewView (fn [x]
                                          (js/console.log "webviewView.onDidChangeVisibility" (.-visible webviewView) x)))))

(defn- postMessage [^js this message]
  (when-let [webviewView (.-view this)]
    (.postMessage (.-webview webviewView) message)))

(defn DataspexViewProvider [extensionUri port]
  (this-as ^js this
           (unchecked-set this "_extensionUri" extensionUri)
           (unchecked-set this "_port" port)
           #js {:resolveWebviewView (partial #'resolveWebviewView this)
                :postMessage (partial #'postMessage this)}))

(defn ^:export activate! [!app-state ^js extension-context port]
  (let [^js provider (DataspexViewProvider (.-extensionUri extension-context) port)]
    (when-contexts/set-context!+ !app-state :calva-power-tools/dataspex-panel-active? true)
    (.push (.-subscriptions extension-context)
           (vscode/window.registerWebviewViewProvider
            "cpt.dataspex.view"
            provider
            #js {:webviewOptions #js {:retainContextWhenHidden true}}))))
