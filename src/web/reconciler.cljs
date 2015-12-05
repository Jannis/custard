(ns web.reconciler
  (:require [om.next :as om]))

(defmulti read om/dispatch)

(defmethod read :requirements
  [{:keys [state query]} key params]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)}))

(defmulti mutate om/dispatch)

(def parser
  (om/parser {:read read :mutate mutate}))

(def initial-state
  {:requirements [{:id "r/foo"
                   :title "Foo"
                   :description "Description of Foo"}
                  {:id "r/bar"
                   :title "Bar"
                   :description "Description of Bar"}]})

(def reconciler
  (om/reconciler {:parser parser
                  :state initial-state}))
