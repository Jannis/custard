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
  [{:keys [custard]} _ {:keys [state]}]
  {:value (or (some->> state
                       second
                       (.state custard)
                       c/requirements)
              [])})

(defmethod readf :components
  [{:keys [custard]} _ {:keys [state]}]
  {:value (or (some->> state
                       second
                       (.state custard)
                       c/components)
              [])})

(defmethod readf :tags
  [{:keys [custard]} _ {:keys [state]}]
  {:value (or (some->> state
                       second
                       (.state custard)
                       c/tags)
              [])})

(defmethod readf :work-items
  [{:keys [custard]} _ {:keys [state]}]
  {:value (or (some->> state
                       second
                       (.state custard)
                       c/work-items)
              [])})

(def parser (om/parser {:read readf :mutate mutatef}))
