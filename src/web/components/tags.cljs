(ns web.components.tags
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(defui Tag
  static om/Ident
  (ident [this props]
    [:tag (:id props)])
  static om/IQuery
  (query [this]
    [:id :title :description])
  Object
  (render [this]
    (let [{:keys [id title description]} (om/props this)]
      description)))

(def tag (om/factory Tag {:key-fn :id}))

(defui Tags
  Object
  (render [this]
    (let [items (:tags (om/props this))]
      (dom/div #js {:className "tags"}
        (for [item items]
          (tag item))))))

(def tags (om/factory Tags {:keyfn :id}))
