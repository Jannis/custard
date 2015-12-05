(ns web.components.components
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(defui Component
  static om/Ident
  (ident [this props]
    [:component (:id props)])
  static om/IQuery
  (query [this]
    [:id :title :description])
  Object
  (render [this]
    (let [{:keys [id title description]} (om/props this)]
      description)))

(def component (om/factory Component {:key-fn :id}))

(defui Components
  Object
  (render [this]
    (let [items (:components (om/props this))]
      (dom/div #js {:className "components"}
        (for [item items]
          (component item))))))

(def components (om/factory Components {:keyfn :id}))
