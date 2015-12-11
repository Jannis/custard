(ns web.app
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [web.reconciler :refer [reconciler start-polling]]
            [web.routing :as routing]
            [web.components.nodes :refer [Node nodes]]
            [web.components.header :refer [header]]
            [web.components.project :refer [Project project]]
            [web.components.state-chooser :refer [StateChooserItem]]))

(enable-console-print!)

(defui App
  static om/IQueryParams
  (params [this]
    {:state [:state "UNCOMMITTED"]})
  static om/IQuery
  (query [this]
    `[;; CUSTARD data
      ({:state ~(om/get-query StateChooserItem)}
       {:state ?state})
      {:states ~(om/get-query StateChooserItem)}
      ({:project ~(om/get-query Project)} {:state ?state})
      ({:requirements ~(om/get-query Node)} {:state ?state})
      ({:components ~(om/get-query Node)} {:state ?state})
      ({:work-items ~(om/get-query Node)} {:state ?state})
      ({:tags ~(om/get-query Node)} {:state ?state})

      ;; UI state
      :view])
  Object
  (select-state [this ident]
    (let [view (:view (om/props this))]
      (routing/activate-route! view {:state (second ident)})))

  (select-view [this view]
    (let [state (:state (om/props this))]
      (routing/activate-route! view {:state (:name state)})))

  (componentWillUpdate [this new-props new-state]
    (let [{:keys [state states]} new-props]
      (if (and (nil? state)
               (not (empty? states)))
        (let [get-fn (fn [id] (first (filter #(= id (:id %)) states)))
              head (get-fn "HEAD")
              master (get-fn "refs/heads/master")]
          (cond
            head (.select-state this [:state "HEAD"])
            master (.select-state this [:state "refs/heads/master"])
            :else (.select-state this [:state (:id (first states))]))))))

  (render [this]
    (dom/div #js {:className "app"}
      (let [props (select-keys (om/props this)
                               [:state :states :view :project])]
        (header
          (om/computed props
                       {:select-state-fn #(.select-state this %)
                        :select-view-fn #(.select-view this %)})))
      (dom/main nil
        (letfn [(render-nodes [view]
                  (nodes {:nodes (get (om/props this) view)}))]
          (condp = (:view (om/props this))
            :project
            (let [project' (:project (om/props this))]
              (if (and (not (nil? project'))
                       (not (empty? project')))
                (project project')
                (dom/div nil "Start by defining a project.")))
            :requirements (render-nodes :requirements)
            :components (render-nodes :components)
            :work-items (render-nodes :work-items)
            :tags (render-nodes :tags)
            (dom/div nil "Nothing selected.")))))))

(defn run []
  (enable-console-print!)
  (om/add-root! reconciler App (gdom/getElement "app"))
  (routing/start!)
  (start-polling))
