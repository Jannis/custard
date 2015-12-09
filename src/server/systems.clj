(ns server.systems
  (:require [adzerk.env :as env] 
            [system.core :refer [defsystem]]
            (system.components
             [jetty :refer [new-web-server]])
            [custard.core :refer [new-custard]]
            [server.handler :refer [app-server backend-server]]))

(env/def APP_PORT (str 3000))
(env/def BACKEND_PORT (str 3001))
(env/def CUSTARD_PATH :required)

(defsystem development-system
  [:app-server (new-web-server (read-string APP_PORT) app-server)
   :backend-server (new-web-server (read-string BACKEND_PORT)
                                   backend-server)
   :custard (new-custard CUSTARD_PATH)])

(defsystem production-system
  [:app-server (new-web-server (read-string APP_PORT) app-server)
   :backend-server (new-web-server (read-string BACKEND_PORT)
                                   backend-server)
   :custard (new-custard CUSTARD_PATH)])
