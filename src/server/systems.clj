(ns server.systems
  (:require [environ.core :refer [env]]
            [system.core :refer [defsystem]]
            (system.components
             [http-kit :refer [new-web-server]])
            [custard.core :refer [new-custard]]
            [server.handler :refer [app-server backend-server]]))

(defsystem development-system
  [:app-server (new-web-server 3000 app-server)
   :backend-server (new-web-server 3001 backend-server)
   :custard (new-custard (env :custard-path))])

(defsystem production-system
  [:app-server (new-web-server 3000 app-server)
   :backend-server (new-web-server 3001 backend-server)
   :custard (new-custard (env :custard-path))])
