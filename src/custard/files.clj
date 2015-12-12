(ns custard.files
  (:import [gitiom.tree Tree]
           [java.io ByteArrayInputStream])
  (:require [clojure.string :as str]
            [gitiom.coerce :refer [to-oid]]
            [gitiom.blob :as git-blob]
            [gitiom.commit :as git-commit]
            [gitiom.tree :as git-tree]
            [me.raynes.fs :as fs]))

(defn relative-path [root path]
  (let [root-segments (fs/split root)
        path-segments (fs/split path)
        n (count root-segments)]
    (str/join "/"
              (condp = (take n path-segments)
                root-segments (drop n path-segments)
                path-segments))))

(defn load-files-from-dir [dir]
  (let [branch? (every-pred fs/directory? (complement fs/hidden?))
        include? (every-pred fs/file? (complement fs/hidden?))]
    (->> dir
         (tree-seq branch? #(.listFiles %))
         (filter include?)
         (map #(vector (relative-path dir %) %))
         (into {}))))

(defn load-files-from-commit [repo commit]
  (let [tree (git-commit/tree repo commit)
        tree? #(= :tree (:type %))
        file? #(= :file (:type %))
        blob? (fn [tree-or-blob]
                (not (instance? Tree tree-or-blob)))
        branch? (fn [tree-or-blob]
                  (instance? Tree tree-or-blob))
        get-path (fn [parent entry]
                   (if (empty? (:path parent))
                     (:name entry)
                     (str/join "/" [(:path parent) (:name entry)])))
        to-tree (fn [parent entry]
                  (when (tree? entry)
                    (let [name (:name entry)
                          path (get-path parent entry)
                          tree (git-tree/get-tree repo parent name)]
                      (assoc tree :path path))))
        to-blob (fn [parent entry]
                  (when (file? entry)
                    (let [name (:name entry)
                          path (get-path parent entry)
                          oid (to-oid repo (:sha1 entry))
                          blob (git-blob/load repo oid)]
                      (assoc blob :path path))))
        to-tree-or-blob (fn [parent entry]
                          (or (to-blob parent entry)
                              (to-tree parent entry)))]
    (->> tree
         (tree-seq branch?
                   (fn [tree]
                     (let [children
                           (->> tree :entries
                                (map #(to-tree-or-blob tree %)))]
                       children)))
         (filter blob?)
         (map #(vector (:path %) (ByteArrayInputStream. (:data %))))
         (into {}))))
