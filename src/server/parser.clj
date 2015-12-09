(ns server.parser
  (:require [om.next.server :as om]
            [custard.core :as c]))

(defmulti readf (fn [env key params] key))

(defmulti mutatef (fn [env key params] key))

(defmethod readf :states
  [{:keys [custard query]} _ _]
  {:value (into []
                (map #(select-keys % query))
                (.states custard))})

(defmethod readf :state
  [{:keys [custard query]} _ {:keys [state]}]
  (let [state (.state custard (second state))]
    {:value (when state (select-keys state query))}))

(defmethod readf :project
  [{:keys [custard]} _ {:keys [state]}]
  {:value (or (some->> state
                       second
                       (.state custard)
                       c/project)
              nil)})

(defmethod readf :requirements
  [{:keys [custard query]} _ {:keys [state]}]
  (let [requirements (or (some->> state
                                  second
                                  (.state custard)
                                  c/requirements)
                         [])]
    {:value (mapv #(select-keys % query) requirements)}))

(defmethod readf :components
  [{:keys [custard query]} _ {:keys [state]}]
  (let [components (or (some->> state
                                second
                                (.state custard)
                                c/components)
                         [])]
    {:value (mapv #(select-keys % query) components)}))

(defmethod readf :work-items
  [{:keys [custard query]} _ {:keys [state]}]
  (let [work-items (or (some->> state
                                second
                                (.state custard)
                                c/work-items)
                       [])]
    {:value (mapv #(select-keys % query) work-items)}))

(defmethod readf :tags
  [{:keys [custard query]} _ {:keys [state]}]
  (let [tags (or (some->> state
                          second
                          (.state custard)
                          c/tags)
                 [])]
    {:value (mapv #(select-keys % query) tags)}))

(def parser (om/parser {:read readf :mutate mutatef}))
