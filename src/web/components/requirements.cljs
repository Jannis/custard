(ns web.components.requirements
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(declare requirement)

(defui Requirement
  static om/Ident
  (ident [this props]
    [:requirement (:name props)])
  static om/IQuery
  (query [this]
    '[:name :title {:children '...}])
  Object
  (render [this]
    (let [{:keys [name title children]} (om/props this)]
      (dom/div #js {:className "requirement"}
        (dom/h2 #js {:className "requirement-header"} title)
        (dom/div #js {:className "requirement-subrequirements"}
          (for [child children]
            (requirement child)))))))

(def requirement (om/factory Requirement {:key-fn :name}))

(defui Requirements
  Object
  (render [this]
    (println (om/props this))
    (let [items (:requirements (om/props this))]
      (dom/div #js {:className "requirements"}
        (for [item items]
          (requirement item))))))

(def requirements (om/factory Requirements))
