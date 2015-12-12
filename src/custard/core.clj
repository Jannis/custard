(ns custard.core
  (:require [com.stuartsierra.component :as component]
            [gitiom.reference :as git-ref]
            [gitiom.repo :as git-repo]
            [juxt.dirwatch :refer [watch-dir close-watcher]]
            [me.raynes.fs :as fs]
            [custard.files :refer [load-files-from-dir
                                   load-files-from-commit]]
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
  (state [this id]))

;;;; State representation

(defrecord State [id name type graph files]
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
    (files path)))

;;;; The uncommitted state (working directory)

(defn uncommitted-state [dir]
  (let [graph (parse-uncommitted dir)
        files (load-files-from-dir dir)]
    (State. "UNCOMMITTED" "UNCOMMITTED" :none graph files)))

;;;; Branch and tag states

(defn ref-states [repo]
  (let [refs (git-ref/load-all repo)]
    (mapv (fn [ref]
            (State. (:name ref)
                    (:name ref)
                    (:type ref)
                    (if (:head ref)
                      (parse-commit repo (:head ref))
                      [])
                    (if (:head ref)
                      (load-files-from-commit repo (:head ref))
                      {})))
          refs)))

;;;; Default Custard implementation

(defrecord Custard [dir repo watcher refs uncommitted]
  ICustard
  (states [this]
    (if @uncommitted
      (into [@uncommitted] @refs)
      @refs))

  (state [this id]
    (if (= id "UNCOMMITTED")
      @uncommitted
      (first (filter #(= id (:id %)) (states this)))))

  component/Lifecycle
  (start [this]
    (let [repo (git-repo/load dir)
          bare? (.isBare (.getRepository repo))]
      (when-not bare?
        (reset! uncommitted (uncommitted-state dir)))
      (reset! refs (ref-states repo))
      (-> this
          (assoc :repo repo)
          (assoc :watcher
                 (watch-dir (fn [_]
                              (when-not bare?
                                (reset! uncommitted
                                        (uncommitted-state dir)))
                              (reset! refs (ref-states repo)))
                            dir)))))

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
                 :refs (atom [])
                 :uncommitted (atom nil)}))
