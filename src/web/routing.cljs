(ns web.routing
  (:require [bidi.bidi :refer [match-route path-for]]
            [bidi.router :refer [set-location! start-router!]]
            [clojure.string :as str]
            [om.next :as om]
            [web.reconciler :refer [reconciler]]))

(enable-console-print!)

(def routes
  ["/"
   {[[#"[^/]*" :state]]
    {"" :project
     "/" {"" :project
          "requirements" :requirements
          "components" :components
          "work-items" :work-items
          "tags" :tags
          "history" :history}}}])

(defn navigate-to [location]
  (let [state (str/replace (:state (:route-params location)) #":" "/")
        view (:handler location)
        app (om/app-root reconciler)]
    (when (and app state)
      (om/set-query! app {:params {:state [:state state]}}))
    (when view
      (om/transact! reconciler `[(app/set-view {:view ~view})]))))

(def router (atom nil))

(defn start! []
  (reset! router
          (start-router! routes
                         {:on-navigate navigate-to
                          :default-location {:handler :project
                                             :route-params
                                             {:state "UNCOMMITTED"}}})))

(defn activate-route! [handler params]
  (let [params' (cond-> params
                  (contains? params :state)
                  (update :state str/replace #"/" ":"))]
    (set-location! @router {:handler handler :route-params params'})))
