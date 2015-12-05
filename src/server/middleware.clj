(ns server.middleware
  (:require [ring.util.response :refer [header]]))

(defn wrap-access-headers
  [handler]
  (fn [request]
    (let [response (handler request)]
      (-> response
          (header "Access-Control-Allow-Origin" "*")
          (header "Access-Control-Allow-Headers" "Accept, Content-Type")
          (header "Access-Control-Allow-Methods"
                  "GET, HEAD, OPTIONS, POST, PUT")))))
