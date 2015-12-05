(ns web.reconciler
  (:require [om.next :as om]))

(defmulti read om/dispatch)

(defmulti mutate om/dispatch)

;;;; CUSTARD data

(defmethod read :states
  [{:keys [state query]} key _]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)}))

(defmethod read :components
  [{:keys [state query]} key params]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)}))

(defmethod read :requirements
  [{:keys [state query]} key params]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)}))

(defmethod read :tags
  [{:keys [state query]} key params]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)}))

(defmethod read :work-items
  [{:keys [state query]} key params]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)}))

;;;; UI state

(defmethod read :view
  [{:keys [state]} key _]
  {:value (or (get @state key) :requirements)})

(defmethod mutate 'app/set-view
  [{:keys [state]} _ {:keys [view]}]
  {:value {:keys [:view]}
   :action #(swap! state assoc :view view)})

(def parser
  (om/parser {:read read :mutate mutate}))

(def initial-state
  {:states [{:id "UNCOMMITTED" :name "UNCOMMITTED" :type :none}
            {:id "HEAD" :name "HEAD" :type :branch}
            {:id "master" :name "master" :type :branch}]
   :requirements [{:id "r/foo"
                   :title "Foo"
                   :description "Description of Foo"}
                  {:id "r/bar"
                   :title "Bar"
                   :description "Description of Bar"}]})

(def reconciler
  (om/reconciler {:parser parser
                  :state initial-state}))
