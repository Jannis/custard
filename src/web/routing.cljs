(ns web.routing
  (:require [bidi.bidi :refer [match-route path-for unmatch-pair tag]]
            [bidi.router :refer [set-location! start-router!]]
            [clojure.string :as str]
            [om.next :as om]
            [web.reconciler :refer [reconciler get-current-state]]))

(enable-console-print!)

(def routes
  ["/"
   {[[#"[^/]*" :state]]
    {"" :project
     "/" {"" :project
          "requirements"
          {"" (tag :requirements :requirements)
           ["#" [#".*" :node]] (tag :requirements :expanded-requirements)}
          "components"
          {"" (tag :components :components)
           ["#" [#".*" :node]] (tag :components :expanded-components)}
          "work-items"
          {"" (tag :work-items :work-items)
           ["#" [#".*" :node]] (tag :work-items :expanded-work-items)}
          "tags"
          {"" (tag :tags :tags)
           ["#" [#".*" :node]] (tag :tags :expanded-tags)}
          "history" :history}}}])

(defn navigate-to [location]
  (let [view (:handler location)
        name (str/replace (:state (:route-params location)) #":" "/")
        state [:state name]
        nodes (str/split (:node (:route-params location)) #",")
        app (om/app-root reconciler)]
    (when (and app state)
      (om/set-query! app {:params {:state state}}))
    (when view
      (om/transact! reconciler `[(app/set-view {:view ~view})]))
    (when-not (empty? nodes)
      (doseq [name nodes]
        (let [ident [:node name]]
          (om/transact! reconciler
                        `[(app/expand-node {:node ~ident})]))))))

(def router (atom nil))

(defn start! []
  (reset! router
          (start-router! routes
                         {:on-navigate navigate-to
                          :default-location {:handler :project
                                             :route-params
                                             {:state "UNCOMMITTED"}}})))

(defn bind-params [params]
  (letfn [(bind-param [res [key value]]
            (assoc res key
                   (if (symbol? value)
                     (case value
                       '?state (second (get-current-state))
                       value)
                     value)))]
    (reduce bind-param {} params)))

(defn activate-route!
  ([route]
   (activate-route! (:handler route) (:route-params route)))
  ([handler params]
   (let [params' (cond-> (bind-params params)
                   (contains? params :node)
                   (update :node (fn [node]
                                   (cond->> node
                                     (sequential? node)
                                     (str/join ","))))
                   (contains? params :state)
                   (update :state str/replace #"/" ":"))]
     (set-location! @router {:handler handler
                             :route-params params'}))))
