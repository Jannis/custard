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

;;;; Parsing

(defn parse-common [data]
  {:title (data "title")
   :description (data "description")
   :mapped-here (mapv #(hash-map :name %) (data "mapped-here"))
   :tags (mapv #(hash-map :name %) (data "tags"))})

(defn parse-project [data]
  {:copyright (data "copyright")})

(defn parse-requirement [data]
  {})

(defn parse-component [data]
  {})

(defn parse-work-item [data]
  {})

(defn parse-tag [data]
  {})

(def kind-parsers
  {"project" parse-project
   "requirement" parse-requirement
   "component" parse-component
   "work-item" parse-work-item
   "tag" parse-tag})

(def kind-aliases
  {"project" ["project"]
   "requirement" ["requirement" "req" "r"]
   "component" ["component" "comp" "c"]
   "work-item" ["work-item" "work" "w"]
   "tag" ["tag" "t"]})

(defn parse-kind [data]
  (when (contains? data "kind")
    (when-let [kind (get data "kind")]
      (ffirst (filter #(some #{kind} (second %)) kind-aliases)))))

(defn custard-node? [node]
  (contains? node :kind))

(defn process-down [node f ctx]
  (let [{:keys [node ctx]} (f node ctx)]
    (-> node
        (update :children
                (fn [children]
                  (mapv #(process-down % f ctx) children))))))

(declare parse-nodes)

(defn parse-children [name-segments parent-name data]
  (letfn [(parse-child [[name-segment data]]
            (parse-nodes (conj name-segments name-segment)
                         parent-name data))]
    (->> data
         (filter #(map? (second %)))
         (map parse-child)
         (apply concat)
         (into []))))

(defn parse-nodes [name-segments parent-name data]
  {:pre [(map? data)]}
  (let [kind (parse-kind data)
        parser (kind-parsers kind)]
    (if (and kind parser)
      (let [name (str/join "/" name-segments)
            basic {:name name
                   :parent parent-name
                   :children (parse-children name-segments name data)}]
        [(merge basic
                 {:kind kind}
                 (parse-common data)
                 (parser data))])
      (parse-children name-segments parent-name data))))

(defn parse-tree [data]
  (letfn [(parse-step [root [name-segment data]]
            (let [nodes (parse-nodes [name-segment] nil data)]
              (update root :children concat nodes)))]
    (let [root {:name nil :children []}
          tree (reduce parse-step root data)]
      tree)))

(defn flatten-tree [node]
  (letfn [(collect-step [m node]
            (merge m (flatten-tree node)))]
    (reduce collect-step
            (if-not (nil? (:name node))
              {(:name node) node}
              {})
            (:children node))))

(defn build-graph [flat-tree]
  (letfn [(node->ident [node]
            [:node (:name node)])
          (node->link [node]
            {:name (:name node)})
          (parse-node [graph node]
            (let [ident (node->ident node)
                  child-links (mapv node->link (:children node))
                  linked-node (assoc node :children child-links)]
              (-> graph
                  (update :nodes conj ident)
                  (assoc-in ident linked-node))))
          (build-step [graph [name node]]
            (parse-node graph node))]
    (reduce build-step {:node {} :nodes []} flat-tree)))

(defn recursive-merge [a b]
  (if (and (map? a) (map? b))
    (merge-with recursive-merge a b)
    (merge a b)))

(defn merge-file-data [m [path data]]
  (update-in m (path->segments path) recursive-merge data))

(defn process-files [path->data]
  (let [data (reduce merge-file-data {} path->data)
        tree (parse-tree data)
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
