(ns web.app
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [web.reconciler :refer [reconciler]]
            [web.remote :as remote]
            [web.routing :as routing]
            [web.components.header :refer [header]]
            [web.components.state :refer [State state]]
            [web.components.state-chooser :refer [StateChooserItem]]))

(enable-console-print!)

(defui App
  static om/IQueryParams
  (params [this]
    {:state [:state "UNCOMMITTED"]})
  static om/IQuery
  (query [this]
    `[;; CUSTARD data
      {:custard/states ~(om/get-query StateChooserItem)}
      ({:custard/state ~(om/get-query State)} {:state ~'?state})

      ;; UI state
      :view])
  Object
  (select-state [this ident]
    (let [view (:view (om/props this))]
      (routing/activate-route! view {:state (second ident)})))

  (select-view [this view]
    (let [state (:custard/state (om/props this))]
      (routing/activate-route! view {:state (:name state)})))

  (componentWillUpdate [this new-props new-state]
    (let [state (:state (om/get-params this))
          states (:custard/states new-props)]
      (println "App: will update:"
               "state?" (not (nil? state))
               "states?" (not (empty? states)))
      (when (not (empty? states))
        (let [names (map :name states)
              find-state (fn [name] (first (filter #(= name %) names)))
              requested-state (find-state (second state))
              head (find-state "HEAD")
              master (find-state "refs/heads/master")]
          (println "App: will update: desired state" state)
          (println "App: will update:   found:" requested-state)
          (when (nil? requested-state)
            (println "App: will update: falling back to HEAD or master")
            (cond
              head (.select-state this [:state "HEAD"])
              master (.select-state this [:state "refs/heads/master"])
              :else (.select-state this [:state (:name (first states))])))))))

  (render [this]
    (dom/div #js {:className "app"}
      (let [header-props [:custard/state :custard/states :View]]
        (header
          (om/computed (select-keys (om/props this) header-props)
                       {:select-state-fn #(.select-state this %)
                        :select-view-fn #(.select-view this %)})))
      (dom/main nil
        (let [custard-state (:custard/state (om/props this))
              view (:view (om/props this))]
          (if custard-state
            (state (om/computed custard-state {:view view}))
            (dom/div nil "Loading...")))))))

(defn run []
  (enable-console-print!)
  (om/add-root! reconciler App (gdom/getElement "app"))
  (routing/start!)
  (remote/connect! reconciler))
