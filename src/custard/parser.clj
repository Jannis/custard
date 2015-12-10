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

(defn set-parent [node ctx]
  (let [parent ctx]
    {:node (assoc node :parent
                  (or (:parent node)
                      (when parent {:name (:name parent)})))
     :ctx (if (custard-node? node) node parent)}))

(declare parse-node)

(defn parse-children [name-segments data]
  (into []
        (comp (filter #(map? (second %)))
              (map (fn [[k v]]
                     (parse-node (conj name-segments k) v))))
        data))

(defn parse-node [name-segments data]
  {:pre [(map? data)]}
  (let [kind (parse-kind data)
        parser (kind-parsers kind)
        basic {:name (str/join "/" name-segments)
               :children (parse-children name-segments data)}]
    (if (and kind parser)
      (merge basic
             {:kind kind}
             (parse-common data)
             (parser data))
      basic)))

(defn parse-tree [data]
  (letfn [(parse-step [root [name-segment data]]
            (let [node (parse-node [name-segment] data)]
              (update root :children conj node)))]
    (let [root {:name [] :children ()}
          tree (reduce parse-step root data)]
      (-> tree
          (process-down set-parent nil)))))

(defn flatten-tree [node]
  (letfn [(collect-step [m node]
            (merge m (flatten-tree node)))]
    (reduce collect-step
            (if (custard-node? node) 
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
                  linked-node (update node :children
                                           #(mapv node->link %))]
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
