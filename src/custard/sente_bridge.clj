(ns custard.sente-bridge
  (:require [clojure.set :refer [difference]]
            [com.stuartsierra.component :as component]
            [om.next.server :as om]
            [reloaded.repl :refer [system]]
            [custard.core :as c]))

;;;; Sente bridge

(defprotocol ISenteBridge
  (states-changed [this key atom old-states new-states]))

(defrecord SenteBridge [custard sente]
  ISenteBridge
  (states-changed [this key atom old-states new-states]
    (println "SenteBridge: states-changed")
    (println "  connected uids" (:connected-uids (:sente system)))
    (let [states (vals new-states)
          filtered (mapv #(select-keys % [:name :revision]) states)
          data {:custard/states filtered}]
      (doseq [uid (:any @(:connected-uids (:sente system)))]
        (println "  notify" uid)
        ((:chsk-send! (:sente system)) uid
         [:custard/notify-states data]))))

  component/Lifecycle
  (start [this]
    (println "SenteBridge: start")
    (add-watch (:states-map custard)
               :sente-bridge-watcher
               (partial states-changed this)))
  (stop [this]
    (println "SenteBridge: stop")
    (remove-watch (:states-map custard) :sente-bridge-watcher)
    (dissoc this :watchers)))

(defn new-sente-bridge []
  (map->SenteBridge {}))

;;;; Om parser

(defn extract-keys [query]
  (letfn [(extract-key [keys expr]
            (cond
              (map? expr) (conj keys (ffirst expr))
              (keyword? expr) (conj keys expr)
              :else keys))]
    (reduce extract-key [] query)))

(defmulti readf (fn [env key params] key))

(defmulti mutatef (fn [env key params] key))

(defmethod readf :custard/states
  [{:keys [custard query]} _ _]
  {:value (into []
                (map #(select-keys % (extract-keys query)))
                (.states custard))})

(defmethod readf :custard/state
  [{:keys [custard parser query]} _ params]
  (let [state (.state custard (second (:state params)))]
    (if state
      (let [state-props [:name :revision :type]
            state-query (filterv #(some #{%} state-props) query)
            nodes-query (into [] (difference (set query)
                                             (set state-query)))
            props (select-keys state state-query)
            nodes (parser {:state state} nodes-query)]
        {:value (merge props nodes)})
      {:value nil})))

(defmethod readf :project
  [{:keys [query state]} _ _]
  (let [project (c/project state)]
    {:value (when project
              (select-keys project (extract-keys query)))}))

(defmethod readf :requirements
  [{:keys [query state]} _ _]
  (let [requirements (or (c/requirements state) [])]
    {:value (mapv #(select-keys % (extract-keys query)) requirements)}))

(defmethod readf :components
  [{:keys [query state]} _ _]
  (let [components (or (c/components state) [])]
    {:value (mapv #(select-keys % (extract-keys query)) components)}))

(defmethod readf :work-items
  [{:keys [query state]} _ _]
  (let [work-items (or (c/work-items state) [])]
    {:value (mapv #(select-keys % (extract-keys query)) work-items)}))

(defmethod readf :tags
  [{:keys [query state]} _ _]
  (let [tags (or (c/tags state) [])]
    {:value (mapv #(select-keys % (extract-keys query)) tags)}))

(def parser
  (om/parser {:read readf :mutate mutatef}))

;;;; Sente event handler

(defmulti sente-event-handler :id)

(defmethod sente-event-handler :custard/query
  [{:keys [event id ?data ring-req ?reply-fn send-fn]}]
  (println "sente-event-handler :custard/query" ?data)
  (when ?data
    (let [result (parser {:custard (:custard system)} ?data)]
      (when ?reply-fn
        (?reply-fn result)))))

(defmethod sente-event-handler :default
  [{:keys [event id ?data ring-req ?reply-fn send-fn]}]
  (println "sente-event-handler :default" event id ?data))
