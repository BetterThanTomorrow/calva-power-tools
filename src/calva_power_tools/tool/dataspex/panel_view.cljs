(ns calva-power-tools.tool.dataspex.panel-view
  (:require
   ["vscode" :as vscode]))

(defn getWebViewHtml []
  "<!DOCTYPE html>
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
        <iframe src='http://localhost:7117/' frameborder='0'
                style='position: absolute; top: 0; left: 0; width: 100%; height: 100%;'
                allow='clipboard-read; clipboard-write'></iframe>
    </body>
    </html>")

(defn- resolveWebviewView [^js this ^js webviewView _context _token]
  (let [webview (.-webview webviewView)]
    (unchecked-set webview "options" #js{:enableScripts true
                                         :localResourceRoots #js [(.-_extensionUri this)]})
    (unchecked-set webview "html" (getWebViewHtml))
    (.onDidReceiveMessage webview (fn [data]
                                    (js/console.log "webview.onDidReceiveMessage" data)))
    (.onDidChangeVisibility webviewView (fn [x]
                                          (js/console.log "webviewView.onDidChangeVisibility" (.-visible webviewView) x)))))

(defn- postMessage [^js this message]
  (when-let [webviewView (.-view this)]
    (.postMessage (.-webview webviewView) message)))

(defn DataspexViewProvider [extensionUri]
  (this-as ^js this
           (unchecked-set this "_extensionUri" extensionUri)
           #js {:resolveWebviewView (partial #'resolveWebviewView this)
                :postMessage (partial #'postMessage this)}))

(defn ^:export activate! [^js extension-context]
  (let [^js provider (DataspexViewProvider (.-extensionUri extension-context))]
    (.push (.-subscriptions extension-context)
           (vscode/window.registerWebviewViewProvider
            "cpt.dataspex"
            provider
            #js {:webviewOptions #js {:retainContextWhenHidden true}}))))
