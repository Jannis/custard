(ns web.remote
  (:require [cemerick.url :refer [url]]
            [cljs.core.async :refer [<! put! chan timeout]]
            [clojure.string :as str]
            [om.next :as om]
            [om.next.protocols :as om-protocols]
            [taoensso.sente :as sente]
            [web.env :as env])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

;;;; Default remote

(def remote
  (let [location (url env/BACKEND_URL "/query")]
    {:host  (if (pos? (:port location))
              (str/join ":" [(:host location) (:port location)])
              (:host location))
     :path (:path location)}))

;;;; Sente setup

(def socket
  (sente/make-channel-socket! (:path remote)
                              {:type :auto :host (:host remote)}))
(def chsk (:chsk socket))
(def ch-chsk (:ch-recv socket))
(def chsk-send! (:send-fn socket))
(def chsk-state (:state socket))

(def ch-pending-queries (chan))

;;;; Merge remote data into Om

(defn merge-remote [merge-fn results]
  (if-not (= :chsk/closed results)
    (merge-fn results)))

;;;; Send to remote function for Om

(defn send-to-remote [queries merge-fn]
  (put! ch-pending-queries {:queries queries :merge-fn merge-fn}))

;;;; Queue queries while backend is down, flush whenever possible

(defn start-flushing-pending-queries! [reconciler]
  (go
    (loop []
      (if (:custard/ready? @reconciler)
        (when-let [data (<! ch-pending-queries)]
          (println "Remote: flush pending queries")
          (chsk-send! [:custard/query (:remote (:queries data))]
                      5000
                      (partial merge-remote (:merge-fn data))))
        (do
          (println "Remote: waiting for connection")
          (<! (timeout 500))))
      (recur))))

;;;; Receive push updates from the server

(defn refetch-root-query [reconciler]
  (let [app (om/app-root reconciler)
        query (om/get-query app)
        env (assoc (:config reconciler) :force-remotes? true)
        sends (om/gather-sends env query [:remote])]
    (when-not (empty? sends)
      (om-protocols/queue-sends! reconciler sends)
      (om/schedule-sends! reconciler))))

(defmulti receive-from-remote (fn [_ data] (first data)))

(defmethod receive-from-remote :custard/notify-states
  [reconciler [_ states]]
  (let [st @reconciler
        state (get-in st (:custard/state st))
        new-state (->> states :custard/states
                       (filter #(= (:name state) (:name %)))
                       first)]
    (if-not (= (:revision state) (:revision new-state))
      (refetch-root-query reconciler)))
  (om/merge! reconciler states))

(defmulti sente-event-handler (fn [_ msg] (:id msg)))

(defmethod sente-event-handler :chsk/recv
  [reconciler {:keys [event ?data]}]
  (when ?data
    (receive-from-remote reconciler ?data)))

(defmethod sente-event-handler :chsk/state
  [reconciler {:keys [?data]}]
  (when (and ?data (or (:open? ?data) (:first-open? ?data)))
    (om/transact! reconciler `[(custard/set-ready {:ready? true})])))

(defmethod sente-event-handler :default
  [reconciler {:keys [event]}]
  (println "Remote: unandled event" event))

(defn connect! [reconciler]
  (start-flushing-pending-queries! reconciler)
  (sente/start-chsk-router!
    ch-chsk (partial sente-event-handler reconciler)))
