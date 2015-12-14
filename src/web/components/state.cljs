(ns web.components.state
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [web.components.nodes :refer [Node nodes sort-nodes]]
            [web.components.project :refer [Project project]]))


(defui State
  static om/Ident
  (ident [this props]
    [:state (:name props)])
  static om/IQuery
  (query [this]
    [:name :revision :type
     {:project (om/get-query Project)}
     {:requirements (om/get-query Node)}
     {:components (om/get-query Node)}
     {:work-items (om/get-query Node)}
     {:tags (om/get-query Node)}])
  Object
  (render [this]
    (let [{:keys [view]} (om/get-computed this)]
      (letfn [(render-nodes [view]
                (let [nodes' (get (om/props this) view)]
                  (nodes {:nodes (sort-nodes nodes')})))]
        (condp = view
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
          (dom/div nil "Not implemented yet."))))))

(def state (om/factory State {:keyfn :name}))
