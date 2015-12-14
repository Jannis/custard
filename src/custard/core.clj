(ns custard.core
  (:require [com.stuartsierra.component :as component]
            [gitiom.reference :as git-ref]
            [gitiom.repo :as git-repo]
            [juxt.dirwatch :refer [watch-dir close-watcher]]
            [me.raynes.fs :as fs]
            [custard.files :refer [load-file-from-dir
                                   load-file-from-commit]]
            [custard.parser :refer [parse-commit parse-uncommitted]]))

;;;; Protocols

(defprotocol IState
  (project [this])
  (requirements [this])
  (components [this])
  (work-items [this])
  (tags [this])
  (file [this path]))

(defprotocol ICustard
  (states [this])
  (state [this name]))

;;;; State representation

(defrecord State [name revision type graph load-file-fn]
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
    (into []
          (comp (map #(get-in graph %))
                (filter #(= "work-item" (:kind %))))
          (:nodes graph)))
  (tags [this]
    (into []
          (comp (map #(get-in graph %))
                (filter #(= "tag" (:kind %))))
          (:nodes graph)))
  (file [this path]
    (load-file-fn path)))

;;;; The uncommitted state (working directory)

(defn uncommitted-state [dir]
  (map->State {:name "UNCOMMITTED"
               :revision (new java.util.Date)
               :type :none
               :graph (parse-uncommitted dir)
               :load-file-fn #(load-file-from-dir dir %)}))

;;;; Branch and tag states

(defn ref-states [repo]
  (let [refs (git-ref/load-all repo)]
    (mapv (fn [ref]
            (map->State {:name (:name ref)
                         :revision (:sha1 (:head ref))
                         :type (:type ref)
                         :graph (if (:head ref)
                                  (parse-commit repo (:head ref))
                                  [])
                         :load-file-fn
                         #(load-file-from-commit repo (:head ref) %)}))
          refs)))

;;;; Default Custard implementation

(defrecord Custard [dir repo watcher states-map]
  ICustard
  (states [this]
    (into [] (vals @states-map)))

  (state [this name]
    (@states-map name))

  component/Lifecycle
  (start [this]
    (let [repo (git-repo/load dir)
          bare? (.isBare (.getRepository repo))]
      (letfn [(reload-states []
                (let [states (if-not bare?
                               {"UNCOMMITTED" (uncommitted-state dir)}
                               {})]
                  (reset! states-map (into states
                                           (map #(vector (:name %) %))
                                           (ref-states repo)))))]
        (reload-states)
        (-> this
            (assoc :repo repo)
            (assoc :watcher (watch-dir (fn [_] (reload-states)) dir))))))

  (stop [this]
    (when watcher
      (close-watcher watcher))
    (-> this
        (dissoc :watcher)
        (dissoc :repo))))

(defn new-custard [path]
  {:pre [(fs/directory? (fs/file path))]}
  (map->Custard {:dir (fs/file path)
                 :repo nil
                 :watcher nil
                 :states-map (atom {})}))
