(ns web.reconciler
  (:import [goog.net XhrIo])
  (:require [cljs.core.async :refer [<! timeout]]
            [clojure.string :as str]
            [cognitect.transit :as transit]
            [om.next :as om]
            [om.next.protocols :as om-protocols]
            [web.env :as env])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;;;; Initial state

(def initial-state {})

;;;; Parser

(defmulti read om/dispatch)

(defmulti mutate om/dispatch)

(def parser
  (om/parser {:read read :mutate mutate}))

;;;; CUSTARD data

(defmethod read :states
  [{:keys [state query]} key _]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)
     :remote true}))

(defmethod read :state
  [{:keys [state query]} key params]
  {:value (get-in @state (:state params))
   :remote true})

(defmethod read :project
  [{:keys [state query]} key params]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)
     :remote true}))

(defmethod read :requirements
  [{:keys [state query]} key params]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)
     :remote true}))

(defmethod read :components
  [{:keys [state query]} key params]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)
     :remote true}))

(defmethod read :tags
  [{:keys [state query]} key params]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)
     :remote true}))

(defmethod read :work-items
  [{:keys [state query]} key params]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)
     :remote true}))

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

(defn merge-remote [results merge-fn]
  (merge-fn results))

(def remotes
  {:remote {:url (str/join "/" [env/BACKEND_URL "query"])
            :callback merge-remote}})

(defn- transit-post [url data cb]
  (.send XhrIo url
         (fn [e]
           (this-as this
             (cb (transit/read (om.transit/reader)
                               (.getResponseText this)))))
         "POST"
         (transit/write (om.transit/writer) data)
         #js {"Content-Type" "application/transit+json"}))

(defn send-to-remotes [remotes sends merge-fn]
  (doseq [[remote query] sends]
    (transit-post (get-in remotes [remote :url])
                  query
                  (fn [data]
                    (let [remote-cb (get-in remotes [remote :callback])]
                      (when remote-cb
                        (remote-cb data merge-fn)))))))

;;;; Reconciler

(def reconciler
  (om/reconciler {:parser parser
                  :state initial-state
                  :merge-tree merge-result-tree
                  :send #(send-to-remotes remotes %1 %2)
                  :remotes (keys remotes)
                  :id-key :id}))

;;;; Backend polling

(defn start-polling []
  (go
    (loop []
      (<! (timeout 5000))
      (let [root (-> reconciler :state deref :root)
            query (om/get-query root)
            cfg (:config reconciler)
            remotes (:remotes cfg)
            sends (om/gather-sends cfg query remotes)]
        (when-not (empty? sends)
          (om-protocols/queue-sends! reconciler sends)
          (om/schedule-sends! reconciler)))
      (recur))))
