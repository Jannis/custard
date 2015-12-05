(ns server.parser
  (:require [om.next.server :as om]))

(defmulti readf (fn [env key params] key))

(defmulti mutatef (fn [env key params] key))

(def parser (om/parser {:read readf :mutate mutatef}))
