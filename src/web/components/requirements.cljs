(ns web.components.requirements
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

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
      (dom/div nil description))))

(def requirement (om/factory Requirement {:key-fn :id}))

(defui Requirements
  Object
  (render [this]
    (let [items (:requirements (om/props this))]
      (dom/div #js {:className "requirements"}
        (for [item items]
          (requirement item))))))

(def requirements (om/factory Requirements {:keyfn :id}))
