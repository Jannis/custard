(ns custard.core
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [juxt.dirwatch :refer [watch-dir close-watcher]]
            [custard.parser :refer [parse-uncommitted]]))

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

(defrecord UncommittedState [id title type graph]
  IState
  (requirements [this])
  (components [this])
  (work-items [this])
  (tags [this]))

(defn uncommitted-state [graph]
  (UncommittedState. "UNCOMMITTED" "UNCOMMITTED" :none graph))

;;;; Default Custard implementation

(defrecord Custard [dir repo watcher uncommitted]
  ICustard
  (states [this]
    (into [(uncommitted-state @uncommitted)]
          []))

  (state [this id]
    (first (filter #(= id (:id %)) (states this))))

  component/Lifecycle
  (start [this]
    (reset! uncommitted (parse-uncommitted dir))
    (assoc this :watcher
           (watch-dir (fn [_]
                        (reset! uncommitted (parse-uncommitted dir)))
                      dir)))

  (stop [this]
    (close-watcher watcher)
    (dissoc this :watcher)))

(defn new-custard [path]
  (map->Custard {:dir (io/as-file path)
                 :repo nil
                 :watcher nil
                 :uncommitted (atom [])}))
