(ns web.components.work-items
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(defui WorkItem
  static om/Ident
  (ident [this props]
    [:work-item (:id props)])
  static om/IQuery
  (query [this]
    [:id :title :description])
  Object
  (render [this]
    (let [{:keys [id title description]} (om/props this)]
      description)))

(def work-item (om/factory WorkItem {:key-fn :id}))

(defui WorkItems
  Object
  (render [this]
    (let [items (:work-items (om/props this))]
      (dom/div #js {:className "work-items"}
        (for [item items]
          (work-item item))))))

(def work-items (om/factory WorkItems {:keyfn :id}))
