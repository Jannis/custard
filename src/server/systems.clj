(ns server.systems
  (:require [adzerk.env :as env] 
            [taoensso.sente.server-adapters.http-kit
             :refer [sente-web-server-adapter]]
            [system.core :refer [defsystem]]
            [com.stuartsierra.component :as component]
            (system.components
             [http-kit :refer [new-web-server]]
             [sente :refer [new-channel-sockets]])
            [custard.core :refer [new-custard]]
            [custard.sente-bridge :refer [new-sente-bridge
                                          sente-event-handler]]
            [server.handler :refer [app-server backend-server]]))

(env/def APP_PORT (str 3000))
(env/def BACKEND_PORT (str 3001))
(env/def CUSTARD_PATH :required)

(defsystem development-system
  [:app-server (new-web-server (read-string APP_PORT) app-server)
   :backend-server (new-web-server (read-string BACKEND_PORT)
                                   backend-server)
   :custard (new-custard CUSTARD_PATH)
   :sente (new-channel-sockets sente-event-handler
                               sente-web-server-adapter)
   :custard-sente-bridge (component/using (new-sente-bridge)
                                          [:custard :sente])])

(defsystem production-system
  [:app-server (new-web-server (read-string APP_PORT) app-server)
   :backend-server (new-web-server (read-string BACKEND_PORT)
                                   backend-server)
   :custard (new-custard CUSTARD_PATH)
   :sente (new-channel-sockets sente-event-handler
                               sente-web-server-adapter)
   :custard-sente-bridge (component/using (new-sente-bridge)
                                          [:custard :sente])])
