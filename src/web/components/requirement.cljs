(ns web.components.requirement
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [web.bootstrap :refer [panel]]))

(defui Requirement
  static om/Ident
  (ident [this props]
    [:requirement (:id props)])
  static om/IQuery
  (query [this]
    [:id :title :description])
  Object
  (render [this]
    (let [{:keys [id title description]} (om/props this)]
      (panel #js {:header title
                  :collapsible true}
        description))))

(def requirement (om/factory Requirement {:key-fn :id}))
