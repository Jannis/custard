(ns custard.core
  (:require [com.stuartsierra.component :as component]))

;;;; Protocols

(defprotocol IState
  (requirements [this])
  (components [this])
  (work-items [this])
  (tags [this]))

(defprotocol ICustard
  (states [this])
  (state [this id]))

;;;; Uncommitted state

(defrecord UncommittedState [id title path graph]
  IState
  (requirements [this])
  (components [this])
  (work-items [this])
  (tags [this]))

(defn uncommitted-state [path]
  (UncommittedState. "UNCOMMITTED" "UNCOMMITTED" path []))

;;;; Default Custard implementation

(defrecord Custard [path repo]
  ICustard
  (states [this]
    (into [(uncommitted-state path)]
          []))

  (state [this id]
    (first (filter #(= id (:id %)) (states this))))

  component/Lifecycle
  (start [this]
    this)

  (stop [this]
    this))

(defn new-custard [path]
  (Custard. path nil))
