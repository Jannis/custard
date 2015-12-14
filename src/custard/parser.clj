(ns custard.parser
  (:require [clojure.string :as str]
            [clojure.walk :refer [keywordize-keys]]
            [clj-yaml.core :as yaml]
            [gitiom.blob :as git-blob]
            [gitiom.commit :as git-commit]
            [gitiom.repo :as git-repo]
            [gitiom.tree :as git-tree]
            [me.raynes.fs :as fs]
            [custard.files :refer [relative-path]]))

;;;; Path utilities

(defn strip-extension [path]
  (last (first (re-seq #"(.*)\..*$" path))))

(defn path->segments [path]
  (fs/split (strip-extension path)))

;;;; Node idents and links

(defn node->ident [node]
  [:node (:name node)])

(defn node->link [node]
  {:name (:name node)})

;;;; Parsing

(def kind-aliases
  {"project" ["project" "p"]
   "requirement" ["requirement" "req" "r"]
   "component" ["component" "comp" "c"]
   "work-item" ["work-item" "work" "w"]
   "tag" ["tag" "t"]})

(defn parse-kind [kind]
  (when-let [kind (cond-> kind (map? kind) (get "kind"))]
    (ffirst (filter #(some #{kind} (second %)) kind-aliases))))

(defn parse-markdown-data [data]
  (let [text (or (data "description") (data "text") "")
        lines (str/split-lines text)
        title-pattern #"#\s+(([a-z]+):\s*)?(.*)"
        tag-pattern #"\+([a-zA-Z0-9-_\/:]+)"]
    (letfn [(parse-md-kind-and-title [res line]
              (if-not (and (:kind res) (:title res))
                (if-let [match (re-matches title-pattern line)]
                  (let [[_ _ kind title] match]
                    (let [res (-> res
                                  (assoc :kind (parse-kind kind))
                                  (assoc :title title))]
                      res))
                  (update res :lines conj line))
                (update res :lines conj line)))
            (parse-md-tags [res line]
              (let [tags (map second (re-seq tag-pattern line))]
                (-> res
                    (dissoc :lines)
                    (update :tags (comp distinct concat) tags))))
            (parse-md-step [res line]
              (merge (parse-md-kind-and-title res line)
                     (parse-md-tags res line)))]
      (let [res (reduce parse-md-step {} lines)
            text (str/join "\n" (reverse (:lines res)))]
        (-> res
            (dissoc :lines)
            (assoc :text text))))))

(defn parse-common [data]
  (let [markdown-data (parse-markdown-data data)]
    {:title (or (data "title") (:title markdown-data))
     :kind (or (parse-kind data) (:kind markdown-data))
     :description (if (:title markdown-data)
                    (:text markdown-data)
                    (or (data "description")
                        (data "text")))
     :mapped-here (mapv #(hash-map :name %) (data "mapped-here"))
     :tags (into []
             (concat (mapv #(hash-map :name %) (data "tags"))
                     (mapv #(hash-map :name %) (:tags markdown-data))))}))

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
  (let [common (parse-common data)
        parser (kind-parsers (:kind common))]
    (if (and (:kind common) parser)
      (let [name (str/join "/" name-segments)
            basic {:name name
                   :parent parent-name
                   :children (parse-children name-segments name data)}]
        [(merge basic
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
  (letfn [(parse-node [graph node]
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

(defn create-mapped-to-links [graph]
  (letfn [(lookup-mapped-to [graph ident]
            (let [nodes (mapv #(get-in graph %) (:nodes graph))]
              (filterv #(some #{ident} (:mapped-here %)) nodes)))
          (create-mapped-to [graph ident]
            (let [node (get-in graph ident)
                  link (node->link node)
                  targets (lookup-mapped-to graph link)]
              (assoc-in graph
                        (conj ident :mapped-to)
                        (mapv node->link targets))))]
    (reduce create-mapped-to graph (:nodes graph))))

(defn create-tagged-links [graph]
  (letfn [(lookup-tag [graph link]
            (let [nodes (mapv #(get-in graph %) (:nodes graph))]
              (filterv #(some #{link} (:tags %)) nodes)))
          (create-tagged [graph ident]
            (let [node (get-in graph ident)]
              (if (= "tag" (:kind node))
                (let [link (node->link node)
                      sources (lookup-tag graph link)]
                  (assoc-in graph
                            (conj ident :tagged)
                            (mapv node->link sources)))
                graph)))]
    (reduce create-tagged graph (:nodes graph))))

(defn process-files [path->data]
  (let [data (reduce merge-file-data {} path->data)
        tree (parse-tree data)
        flat-tree (flatten-tree tree)
        graph (build-graph flat-tree)]
    (-> graph
        create-mapped-to-links
        create-tagged-links)))

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
