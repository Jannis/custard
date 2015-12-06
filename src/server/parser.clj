(ns server.parser
  (:require [om.next.server :as om]
            [custard.core :as c]))

(defmulti readf (fn [env key params] key))

(defmulti mutatef (fn [env key params] key))

(defmethod readf :states
  [{:keys [custard]} _ _]
  {:value (into []
                (map #(select-keys % [:id :title :type]))
                (c/states custard))})

(defmethod readf :requirements
  [{:keys [custard]} _ {:keys [state]}]
  {:value (or (some->> state
                       second
                       (c/state custard)
                       c/requirements)
              [])})

(defmethod readf :components
  [{:keys [custard]} _ {:keys [state]}]
  {:value (or (some->> state
                       second
                       (c/state custard)
                       c/components)
              [])})

(defmethod readf :tags
  [{:keys [custard]} _ {:keys [state]}]
  {:value (or (some->> state
                       second
                       (c/state custard)
                       c/tags)
              [])})

(defmethod readf :work-items
  [{:keys [custard]} _ {:keys [state]}]
  {:value (or (some->> state
                       second
                       (c/state custard)
                       c/work-items)
              [])})

(def parser (om/parser {:read readf :mutate mutatef}))
