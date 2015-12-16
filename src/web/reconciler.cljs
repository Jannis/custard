(ns web.reconciler
  (:require [om.next :as om]
            [om.next.protocols :as om-protocols]
            [web.remote :as remote]))

;;;; Initial state

(def initial-state {})

;;;; Parser

(defmulti read om/dispatch)

(defmulti mutate om/dispatch)

(def parser
  (om/parser {:read read :mutate mutate}))

;;;; CUSTARD data

(defn go-remote? [st params]
  (let [current-state (:custard/state st)]
    (not= (:state params) current-state)))

(def db->tree-cached (memoize om/db->tree))

(defmethod read :custard/states
  [{:keys [state query]} key _]
  (let [st @state]
    {:value (db->tree-cached query (get st key) st)
     :remote (empty? (:custard/states st))}))

(defmethod read :custard/state
  [{:keys [force-remotes? query state] :as env} key params]
  (let [st @state]
    {:value (db->tree-cached query (get st key) st)
     :remote (or force-remotes? (go-remote? st params))}))

(defmethod read :node
  [{:keys [query query-root state]} _ _]
  {:value (om/db->tree query query-root @state)})

(defmethod read :custard/ready?
  [{:keys [state]} key _]
  {:value (boolean (get @state key))})

(defmethod mutate 'custard/set-ready
  [{:keys [state]} _ {:keys [ready?]}]
  {:value {:keys [:custard/ready?]}
   :action #(swap! state assoc :custard/ready? ready?)})

;;;; UI state

(defmethod read :view
  [{:keys [state]} key _]
  {:value (or (get @state key) :project)})

(defmethod mutate 'app/set-view
  [{:keys [state]} _ {:keys [view]}]
  {:value {:keys [:view]}
   :action #(swap! state assoc :view view)})

(defmethod mutate 'app/expand-node
  [{:keys [state]} _ {:keys [node]}]
  {:value {:keys [node]}
   :action #(swap! state assoc-in (conj node :ui/expanded) true)})

(defmethod mutate 'app/toggle-node-expanded
  [{:keys [state]} _ {:keys [node]}]
  {:value {:keys [node]}
   :action #(swap! state update-in (conj node :ui/expanded) not)})

;;;; Remotes

(defn merge-result-tree [a b]
  (letfn [(merge-tree [a b]
            (if (and (map? a) (map? b))
              (merge-with #(merge-tree %1 %2) a b)
              b))]
    (merge-tree a b)))

;;;; Reconciler

(def reconciler
  (om/reconciler {:parser parser
                  :state initial-state
                  :pathopt true
                  :merge-tree merge-result-tree
                  :send remote/send-to-remote
                  :id-key :id}))

(defn get-current-state []
  (:custard/state @reconciler))
