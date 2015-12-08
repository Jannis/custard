(ns server.handler
  (:import [java.io ByteArrayOutputStream]
           [net.sourceforge.plantuml
            FileFormat FileFormatOption SourceStringReader])
  (:require [clojure.core.memoize :as memoize]
            [clojure.data.codec.base64 :as base64]
            [cognitect.transit :as transit]
            [compojure.core :refer [defroutes GET OPTIONS POST]]
            [compojure.route :as route]
            [om.next.server :as om]
            [ring.util.response :refer [content-type header response]]
            [ring.middleware.format-params
             :refer [wrap-transit-json-params]]
            [ring.middleware.format-response
             :refer [wrap-transit-json-response]]
            [reloaded.repl :refer [system]]
            [server.middleware :refer [wrap-access-headers]]
            [server.parser :refer [parser]]))

;;;; App server

(defroutes app-routes
  (route/resources "/" {:root "."})
  (route/not-found "Not found"))

(def app-server
  (-> app-routes))

;;;; Backend server

(defn handle-query [params]
  (let [ret (parser {:custard (:custard system)} params)]
    (println ">>" ret)
    (response ret)))

(defn handle-echo [params]
  (println "<<" params (type params))
  (println ">>" params)
  (response params))

(defn generate-uml-svg [uml]
  (let [reader (SourceStringReader. uml)
        format (FileFormatOption. FileFormat/SVG)]
    (with-open [stream (ByteArrayOutputStream.)]
      (.generateImage reader stream format)
      (String. (.toByteArray stream)))))

(def generate-uml-svg-cached
  (memoize/lu generate-uml-svg :lu/threshold 100))

(defn handle-uml [encoded]
  (let [uml (String. (base64/decode (.getBytes encoded)))
        svg (generate-uml-svg-cached uml)]
    (-> (response svg)
        (content-type "image/svg+xml"))))

(defroutes backend-routes
  (OPTIONS "/query"     {params :body-params} (handle-query params))
  (POST    "/query"     {params :body-params} (handle-query params))
  (OPTIONS "/echo"      {params :body-params} (handle-echo params))
  (POST    "/echo"      {params :body-params} (handle-echo params))
  (GET     "/uml/:data" [data] (handle-uml data))
  (route/not-found "Not found"))

(defn make-om-transit-decoder []
  (fn [in]
    (transit/read (om/reader in))))

(defn make-om-transit-encoder []
  (fn [in]
    (let [out (ByteArrayOutputStream.)]
      (transit/write (om/writer out) in)
      (.toByteArray out))))

(def backend-server
  (-> backend-routes
      (wrap-access-headers)
      (wrap-transit-json-params :decoder (make-om-transit-decoder)
                                :options {:verbose true})
      (wrap-transit-json-response :encoder (make-om-transit-encoder)
                                  :options {:verbose true})))
