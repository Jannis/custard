(ns web.reconciler
  (:import [goog.net XhrIo])
  (:require [cognitect.transit :as transit]
            [om.next :as om]))

(defmulti read om/dispatch)

(defmulti mutate om/dispatch)

;;;; CUSTARD data

(defmethod read :states
  [{:keys [state query]} key _]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)
     :remote true}))

(defmethod read :components
  [{:keys [state query]} key params]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)
     :remote true}))

(defmethod read :requirements
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

(defn merge-result-tree [a b]
  (letfn [(merge-tree [a b]
            (if (and (map? a) (map? b))
              (merge-with #(merge-tree %1 %2) a b)
              b))]
    (merge-tree a b)))

(defn merge-remote [results merge-fn]
  (println "<<" results)
  (merge-fn results))

(def remotes
  {:remote {:url "http://localhost:3001/query"
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
  (println ">> send" remotes "sends" sends)
  (doseq [[remote query] sends]
    (transit-post (get-in remotes [remote :url])
                  query
                  (fn [data]
                    (let [remote-cb (get-in remotes [remote :callback])]
                      (when remote-cb
                        (remote-cb data merge-fn)))))))

(def reconciler
  (om/reconciler {:parser parser
                  :state initial-state
                  :merge-tree merge-result-tree
                  :send #(send-to-remotes remotes %1 %2)
                  :remotes (keys remotes)
                  :id-key :id}))
