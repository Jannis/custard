(ns custard.parser
  (:require [clojure.string :as str]
            [clojure.walk :refer [keywordize-keys]]
            [clj-yaml.core :as yaml]
            [me.raynes.fs :as fs]))

(defn normalize-filename [file]
  (last (first (re-seq #"(.*)\..*$" file))))

(defn relative-filename [root file]
  (let [root-segments (fs/split root)
        file-segments (fs/split file)
        root-size (count root-segments)]
    (str/join "/"
              (condp = (take root-size file-segments)
                root-segments (drop root-size file-segments)
                file-segments))))

(defn process-filename [[filename data]]
  (let [name-segments (-> filename
                          normalize-filename
                          (str/split #"/"))]
    [name-segments data]))

(defn recursive-merge [a b]
  (if (and (map? a) (map? b))
    (merge-with recursive-merge a b)
    (merge a b)))

(defn parse-step [tree [name-segments data]]
  (update-in tree name-segments recursive-merge data))

;;;; Node tree

(defn normalize-kind [node]
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
    (-> (if (contains? node "kind")
          (update node "kind" normalize)
          node)
        (update :children (fn [children]
                            (mapv #(normalize-kind %)
                                  children))))))

(defn inject-name [node parent-segments]
  (let [segments (if (nil? (:name node))
                   parent-segments
                   (conj parent-segments (:name node)))]
    (-> node
        (assoc :name (str/join "/" segments))
        (update :children (fn [children]
                            (mapv #(inject-name % segments)
                                  children))))))

(defn inject-parent [node parent]
  (let [next-parent (if (contains? node "kind") node parent)]
    (-> node
        (assoc :parent (or (:parent node) (:name parent)))
        (update :children (fn [children]
                            (mapv #(inject-parent % next-parent)
                                  children))))))

(defn build-tree [root data]
  (letfn [(build-node [name-segment data]
            (cond
              (map? data)
              (into {:name name-segment
                     :children (->> data
                                    (map (fn [[k v]]
                                           (if (map? v)
                                             (build-node k v))))
                                    (keep identity)
                                    (into []))}
                    (remove #(map? (second %)) data))))
          (build-step [root [name-segment data]]
            (update root :children conj
                    (build-node name-segment data)))]
    (let [tree (reduce build-step root data)]
      (-> tree
          (normalize-kind)
          (inject-name [])
          (inject-parent nil)))))

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
                  linked-data (-> node
                                  (dissc :parent)
                                  (update :children #(mapv node->link %))
                                  keywordize-keys)]
              (-> graph
                  (update :nodes conj ident)
                  (assoc-in ident linked-data))))
          (build-step [graph [name node]]
            (build-node graph node))]
    (reduce build-step {:node {} :nodes []} flat-tree)))

(defn parse-files [files]
  (let [data (->> files
                  (map process-filename)
                  (reduce parse-step {}))
        tree (build-tree {:children []} data)
        flat-tree (flatten-tree tree)
        graph (build-graph flat-tree)]
    graph))

(defn parse-uncommitted [dir]
  (let [files (->> (fs/find-files dir #".*\.yaml$")
                   (filter #(fs/file? %)))]
    (parse-files (->> files
                      (map #(vector (relative-filename dir %)
                                    (-> %
                                        slurp
                                        (yaml/parse-string false))))
                      (into {})))))
