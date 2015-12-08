(ns server.core
  (:gen-class)
  (:require [reloaded.repl :refer [go set-init!]]
            [server.systems :refer [production-system]]))

(defn -main [& args]
  (let [system (or (first args) #'production-system)]
    (set-init! system)
    (go)))
