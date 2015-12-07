(ns custard.core
  (:require [com.stuartsierra.component :as component]
            [juxt.dirwatch :refer [watch-dir close-watcher]]
            [me.raynes.fs :as fs]
            [custard.parser :refer [parse-uncommitted]]))

;;;; Protocols

(defprotocol IState
  (project [this])
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
  (project [this]
    (first (into []
                 (comp (map #(get-in graph %))
                       (filter #(= "project" (:kind %))))
                 (:nodes graph))))
  (requirements [this]
    (into []
          (comp (map #(get-in graph %))
                (filter #(= "requirement" (:kind %))))
          (:nodes graph)))
  (components [this]
    (into []
          (comp (map #(get-in graph %))
                (filter #(= "component" (:kind %))))
          (:nodes graph)))
  (work-items [this]
    (mapv #(get-in graph %) (:work-items graph)))
  (tags [this]
    (mapv #(get-in graph %) (:tags graph))))

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
    (when watcher
      (close-watcher watcher))
    (dissoc this :watcher)))

(defn new-custard [path]
  {:pre [(fs/directory? (fs/file path))]}
  (map->Custard {:dir (fs/file path)
                 :repo nil
                 :watcher nil
                 :uncommitted (atom [])}))
