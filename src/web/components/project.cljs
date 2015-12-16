(ns web.components.project
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [web.components.markdown :refer [markdown]]))

(defui Project
  static om/Ident
  (ident [this props]
    [:node (:name props)])
  static om/IQuery
  (query [this]
    [:name :title :description :copyright :sort-by])
  Object
  (render [this]
    (let [{:keys [name title description
                  copyright sort-by]} (om/props this)]
      (dom/div #js {:className "node node-root"}
        (dom/h2 #js {:className "node-header"} title)
        (if description
          (dom/div #js {:className "project-description"}
            (markdown {:text description})))
        (if copyright
          (dom/div #js {:className "project-copyright"}
            (dom/h3 #js {:className "project-copyright-header"}
              "Copyright")
            (dom/div #js {:className "project-copyright-text"}
              (markdown {:text copyright}))))))))

(def project (om/factory Project {:key-fn :name}))
