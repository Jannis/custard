(ns web.components.node-link
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(defui Badge
  Object
  (render [this]
    (let [{:keys [kind]} (om/props this)]
      (dom/span #js {:className "badge"}
        (case kind
          "requirement" "r"
          "component" "c"
          "work-item" "w"
          "tag" "t"
          "?")))))

(def badge (om/factory Badge))

(defui NodeLink
  static om/Ident
  (ident [this props]
    [:node (:name props)])
  static om/IQuery
  (query [this]
    [:name :title :kind])
  Object
  (render [this]
    (let [{:keys [name title kind]} (om/props this)]
      (dom/div #js {:className "node-link"}
        (badge {:kind kind})
        (dom/span #js {:className "node-link-title"} title)
        (dom/span #js {:className "node-link-name"} name)))))

(def node-link (om/factory NodeLink {:key-fn :name}))
