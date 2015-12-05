(ns custard.core
  (:require [com.stuartsierra.component :as component]))

(defprotocol IState)

(defrecord UncommittedState
  IState)

(defrecord Custard [path repo]
  component/Lifecycle
  (start [this]
    this)

  (stop [this]
    this))

(defn new-custard [path]
  (Custard. path nil))
