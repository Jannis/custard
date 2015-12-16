(ns web.components.state
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [web.components.nodes :refer [Node nodes sort-nodes]]
            [web.components.project :as project-component]))


(defui State
  static om/Ident
  (ident [this props]
    [:state (:name props)])
  static om/IQuery
  (query [this]
    [:name :revision :type
     {:project (om/get-query project-component/Project)}
     {:requirements (om/get-query Node)}
     {:components (om/get-query Node)}
     {:work-items (om/get-query Node)}
     {:tags (om/get-query Node)}])
  Object
  (render [this]
    (let [{:keys [project]} (om/props this)
          {:keys [view]} (om/get-computed this)]
      (letfn [(render-nodes [view]
                (let [nodes' (get (om/props this) view)]
                  (nodes {:nodes nodes'
                          :sort-by (or (:sort-by project)
                                       "location")})))]
        (condp = view
          :project
          (if (and (not (nil? project))
                   (not (empty? project)))
            (project-component/project project)
            (dom/div nil "Start by defining a project."))
          :requirements (render-nodes :requirements)
          :components (render-nodes :components)
          :work-items (render-nodes :work-items)
          :tags (render-nodes :tags)
          (dom/div nil "Not implemented yet."))))))

(def state (om/factory State {:keyfn :name}))
