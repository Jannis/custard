(ns web.app
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [web.reconciler :refer [reconciler start-polling]]
            [web.components.components :refer [Component components]]
            [web.components.header :refer [header]]
            [web.components.project :refer [Project project]]
            [web.components.requirements :refer [Requirement
                                                 requirements]]
            [web.components.state-chooser :refer [StateChooserItem]]
            [web.components.tags :refer [Tag tags]]
            [web.components.work-items :refer [WorkItem work-items]]))

(enable-console-print!)

(defui App
  static om/IQueryParams
  (params [this]
    {:state [:state "UNCOMMITTED"]})
  static om/IQuery
  (query [this]
    `[;; CUSTARD data
      {:states ~(om/get-query StateChooserItem)}
      ({:project ~(om/get-query Project)} {:state ?state})
      ({:requirements ~(om/get-query Requirement)} {:state ?state})
      ({:components ~(om/get-query Component)} {:state ?state})
      ({:work-items ~(om/get-query WorkItem)} {:state ?state})
      ({:tags ~(om/get-query Tag)} {:state ?state})

      ;; UI state
      :view])
  Object
  (select-state [this ident]
    (om/set-query! this {:params {:state ident}}))

  (select-view [this view]
    (om/transact! this `[(app/set-view {:view ~view})]))

  (render [this]
    (dom/div #js {:className "app"}
      (let [props (select-keys (om/props this) [:states :view :project])]
        (header
          (om/computed props
                       {:select-state-fn #(.select-state this %)
                        :select-view-fn #(.select-view this %)})))
      (dom/main nil
        (condp = (:view (om/props this))
          :project
          (let [project' (:project (om/props this))]
            (if (and (not (nil? project'))
                     (not (empty? project')))
              (project project')
              (dom/div nil "Start by defining a project.")))

          :requirements
          (let [items (select-keys (om/props this) [:requirements])]
            (requirements items))

          :components
          (let [items (select-keys (om/props this) [:components])]
            (components items))

          :work-items
          (let [items (select-keys (om/props this) [:work-items])]
            (work-items items))

          :tags
          (let [items (select-keys (om/props this) [:tags])]
            (tags items))

          (dom/div nil "Nothing selected."))))))

(defn run []
  (enable-console-print!)
  (om/add-root! reconciler App (gdom/getElement "app"))
  (start-polling))
