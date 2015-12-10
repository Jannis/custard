(ns custard.parser
  (:require [clojure.string :as str]
            [clojure.walk :refer [keywordize-keys]]
            [clj-yaml.core :as yaml]
            [gitiom.blob :as git-blob]
            [gitiom.commit :as git-commit]
            [gitiom.repo :as git-repo]
            [gitiom.tree :as git-tree]
            [me.raynes.fs :as fs]))

;;;; Path utilities

(defn strip-extension [path]
  (last (first (re-seq #"(.*)\..*$" path))))

(defn relative-path [root path]
  (let [root-segments (fs/split root)
        path-segments (fs/split path)
        n (count root-segments)]
    (str/join "/"
              (condp = (take n path-segments)
                root-segments (drop n path-segments)
                path-segments))))

(defn path->segments [path]
  (fs/split (strip-extension path)))

(defn recursive-merge [a b]
  (if (and (map? a) (map? b))
    (merge-with recursive-merge a b)
    (merge a b)))

(defn parse-step [tree [name-segments data]]
  (update-in tree name-segments recursive-merge data))

;;;; Node tree

(defn process-down [node f ctx]
  (let [{:keys [node ctx]} (f node ctx)]
    (-> node
        (update :children
                (fn [children]
                  (mapv #(process-down % f ctx) children))))))

(defn set-kind [node _]
  (letfn [(normalize [kind]
            (let [aliases {"project" ["project"]
                           "requirement" ["requirement" "req" "r"]
                           "component" ["component" "comp" "c"]
                           "work-item" ["work-item" "work" "w"]
                           "tag" ["tag" "t"]}
                  res (or (->> aliases
                               (filter #(some #{kind} (second %)))
                               ffirst)
                          kind)]
              res))]
    {:node (if (contains? node "kind")
             (update node "kind" normalize)
             node)}))

(defn set-parent [node ctx]
  (let [parent ctx]
    {:node (assoc node :parent (or (:parent node)
                                   (:name parent)))
     :ctx (if (contains? node "kind") node parent)}))

(defn create-mapped-here-links [node _]
  {:node (update node "mapped-here"
                 (fn [mapped-here]
                   (mapv #(hash-map :name %) mapped-here)))})

(defn build-tree [data]
  (letfn [(build-children [name-segments data]
            (into []
                  (comp (filter #(map? (second %)))
                        (map (fn [[k v]]
                               (build-node (conj name-segments k) v))))
                  data))
          (build-node [name-segments data]
            (cond
              (map? data)
              (into {:name (str/join "/" name-segments)
                     :children (build-children name-segments data)}
                    (remove #(map? (second %)) data))))
          (build-step [root [name-segment data]]
            (update root :children conj
                    (build-node [name-segment] data)))]
    (let [root {:name [] :children ()}
          tree (reduce build-step root data)]
      (-> tree
          (process-down set-kind nil)
          (process-down set-parent nil)
          (process-down create-mapped-here-links nil)))))

(defn flatten-tree [node]
  (letfn [(collect-step [m node]
            (merge m (flatten-tree node)))]
    (reduce collect-step
            (if (contains? node "kind")
              {(:name node) node}
              {})
            (:children node))))

(defn build-graph [flat-tree]
  (letfn [(node->ident [node]
            [:node (:name node)])
          (node->link [node]
            {:name (:name node)})
          (build-node [graph node]
            (let [ident (node->ident node)
                  children (:children node)
                  parent (if (nil? (:parent node))
                           nil
                           {:name (:parent node)})
                  linked-data (-> node
                                  (assoc :parent parent)
                                  (update :children #(mapv node->link %))
                                  keywordize-keys)]
              (-> graph
                  (update :nodes conj ident)
                  (assoc-in ident linked-data))))
          (build-step [graph [name node]]
            (build-node graph node))]
    (reduce build-step {:node {} :nodes []} flat-tree)))

(defn recursive-merge [a b]
  (if (and (map? a) (map? b))
    (merge-with recursive-merge a b)
    (merge a b)))

(defn merge-file-data [m [path data]]
  (update-in m (path->segments path) recursive-merge data))

(defn process-files [path->data]
  (let [data (reduce merge-file-data {} path->data)
        tree (build-tree data)
        flat-tree (flatten-tree tree)
        graph (build-graph flat-tree)]
    graph))

(defn parse-yaml [data]
  (try
    (yaml/parse-string data false)
    (catch Exception e
      {:error (str e)})))

(defn parse-uncommitted [dir]
  (let [files (filter fs/file? (fs/find-files dir #".*\.yaml$"))
        paths (map #(relative-path dir %) files)
        datas (map #(parse-yaml (slurp %)) files)
        path->data (zipmap paths datas)]
    (process-files path->data)))

(defn parse-commit [repo commit]
  (let [tree (git-commit/tree repo commit)
        walk (git-tree/walk repo [tree] true)
        lazy-walk (take-while #(.next %) (repeat walk))]
    (letfn [(parse-entry [res walk]
              (if (and (not (.isSubtree walk))
                       (re-matches #".*\.yaml$" (.getNameString walk)))
                (let [oid (.getObjectId walk 0)
                      blob (git-blob/load repo oid)
                      data (String. (:data blob))]
                  (assoc res
                         (.getPathString walk)
                         (parse-yaml data)))
                res))]
      (process-files (reduce parse-entry {} lazy-walk)))))
