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

;;;; Merge remote data into Om

(defn merge-remote [merge-fn results]
  (println "merge-remote" (if (map? results) (keys results)))
  (if-not (= :chsk/closed results)
    (merge-fn results)))

;;;; Send to remote function for Om

(defn send-to-remote [queries merge-fn]
  (println "send-to-remote" (:remote queries))
  (chsk-send! [:custard/query (:remote queries)]
              5000
              (partial merge-remote merge-fn)))

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
    (println "old" (:revision state) "new" (:revision new-state))
    (if-not (= (:revision state) (:revision new-state))
      (refetch-root-query reconciler)))
  (om/merge! reconciler states))

(defmulti sente-event-handler (fn [_ msg] (:id msg)))

(defmethod sente-event-handler :chsk/recv
  [reconciler {:keys [event ?data]}]
  (when ?data
    (receive-from-remote reconciler ?data)))

(defmethod sente-event-handler :default
  [reconciler msg]
  (println "sente-event-handler :default" msg))

(defn start-sente-event-handler! [reconciler]
  (sente/start-chsk-router!
    ch-chsk (partial sente-event-handler reconciler)))
