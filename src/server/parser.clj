(ns server.parser
  (:require [om.next.server :as om]
            [custard.core :as c]))

(defn extract-keys [query]
  (letfn [(extract-key [keys expr]
            (cond
              (map? expr) (conj keys (ffirst expr))
              (keyword? expr) (conj keys expr)
              :else keys))]
    (reduce extract-key [] query)))

(defmulti readf (fn [env key params] key))

(defmulti mutatef (fn [env key params] key))

(defmethod readf :states
  [{:keys [custard query]} _ _]
  {:value (into []
                (map #(select-keys % (extract-keys query)))
                (.states custard))})

(defmethod readf :state
  [{:keys [custard query]} _ {:keys [state]}]
  (let [state (.state custard (second state))]
    {:value (when state (select-keys state (extract-keys query)))}))

(defmethod readf :project
  [{:keys [custard query]} _ {:keys [state]}]
  (let [project (some->> state
                         second
                         (.state custard)
                         c/project)]
    {:value (when project
              (select-keys project (extract-keys query)))}))

(defmethod readf :requirements
  [{:keys [custard query]} _ {:keys [state]}]
  (let [requirements (or (some->> state
                                  second
                                  (.state custard)
                                  c/requirements)
                         [])]
    {:value (mapv #(select-keys % (extract-keys query))
                  requirements)}))

(defmethod readf :components
  [{:keys [custard query]} _ {:keys [state]}]
  (let [components (or (some->> state
                                second
                                (.state custard)
                                c/components)
                         [])]
    {:value (mapv #(select-keys % (extract-keys query))
                  components)}))

(defmethod readf :work-items
  [{:keys [custard query]} _ {:keys [state]}]
  (let [work-items (or (some->> state
                                second
                                (.state custard)
                                c/work-items)
                       [])]
    {:value (mapv #(select-keys % (extract-keys query))
                  work-items)}))

(defmethod readf :tags
  [{:keys [custard query]} _ {:keys [state]}]
  (let [tags (or (some->> state
                          second
                          (.state custard)
                          c/tags)
                 [])]
    {:value (mapv #(select-keys % (extract-keys query))
                  tags)}))

(def parser (om/parser {:read readf :mutate mutatef}))
