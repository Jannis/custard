(ns server.handler
  (:import [java.io ByteArrayOutputStream]
           [net.sourceforge.plantuml
            FileFormat FileFormatOption SourceStringReader])
  (:require [clojure.core.memoize :as memoize]
            [clojure.data.codec.base64 :as base64]
            [clojure.string :as str]
            [compojure.core :refer [defroutes GET OPTIONS POST]]
            [compojure.route :as route]
            [pantomime.mime :refer [mime-type-of]]
            [ring.util.response :refer [content-type
                                        header
                                        response
                                        resource-response]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [reloaded.repl :refer [system]]
            [taoensso.sente :as sente]
            [custard.core :as c]
            [server.middleware :refer [wrap-access-headers]]))

;;;; App server

(defroutes app-routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (route/resources "/")
  (route/not-found "Not found"))

(def app-server
  (-> app-routes))

;;;; Backend server

;;; PlantUML service

(defn generate-uml-svg [uml]
  (let [reader (SourceStringReader. uml)
        format (FileFormatOption. FileFormat/SVG)]
    (with-open [stream (ByteArrayOutputStream.)]
      (.generateImage reader stream format)
      (String. (.toByteArray stream)))))

(def generate-uml-svg-cached
  (memoize/lu generate-uml-svg :lu/threshold 100))

(defn base64-decode [data]
  (let [normalized (str/replace data
                                #"-|_"
                                {"-" "+"
                                 "_" "/"})]
    (String. (base64/decode (.getBytes normalized)))))

(defn handle-uml [encoded]
  (let [uml (base64-decode encoded)
        svg (generate-uml-svg-cached uml)]
    (-> (response svg)
        (content-type "image/svg+xml"))))

;;; Versioned files service

(defn handle-file [state-name path]
  (let [state-name' (str/replace state-name #":" "/")
        state (c/state (:custard system) state-name')
        stream (some-> state (c/file path))
        mime-type (mime-type-of path)]
    (when stream
      (-> (response stream)
          (content-type mime-type)))))

;;; Backend routes

(defn ajax-get-or-ws-handshake [req]
  ((:ring-ajax-get-or-ws-handshake (:sente system)) req))

(defn ajax-post [req]
  ((:ring-ajax-post (:sente system)) req))

(defroutes backend-routes
  (GET  "/query"             req          (ajax-get-or-ws-handshake req))
  (POST "/query"             req          (ajax-post req))
  (GET  "/uml/:data"         [data]       (handle-uml data))
  (GET  "/file/:state/:path" [state path] (handle-file state path))

  (route/not-found "Not found"))

;;; Wiring it all up

(def backend-server
  (-> backend-routes
      (wrap-keyword-params)
      (wrap-params)
      (wrap-access-headers)))
