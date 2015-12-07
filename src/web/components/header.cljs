(ns web.components.header
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [web.components.state-chooser :refer [state-chooser]]))

(defui HeaderNavItem
  Object
  (render [this]
    (let [{:keys [id name view select-fn]} (om/props this)]
      (dom/a #js {:className (str "header-nav-item"
                                  (when (= id view)
                                    " header-nav-item-active"))
                  :onClick (fn [e]
                             (when select-fn
                               (select-fn id))
                             (.preventDefault e))}
        name))))

(def header-nav-item (om/factory HeaderNavItem {:keyfn :id}))

(defui Header
  Object
  (render [this]
    (let [{:keys [state states view]} (om/props this)
          {:keys [select-state-fn
                  select-view-fn]} (om/get-computed this)]
      (dom/header #js {:className "header"}
        (dom/div #js {:className "header-title"}
          (dom/h1 #js {:className "header-title-text"
                       :onClick #(when select-view-fn
                                   (select-view-fn :project))}
            "CUSTARD")
          (dom/nav #js {:className "header-title-nav"}
            (state-chooser {:state state
                            :states states
                            :select-fn select-state-fn})))
        (dom/nav #js {:className "header-nav"}
          (header-nav-item {:id :requirements
                            :name "Requirements"
                            :view view
                            :select-fn select-view-fn})
          (header-nav-item {:id :components
                            :name "Architecture"
                            :view view
                            :select-fn select-view-fn})
          (header-nav-item {:id :work-items
                            :name "Work Items"
                            :view view
                            :select-fn select-view-fn})
          (header-nav-item {:id :tags
                            :name "Tags"
                            :view view
                            :select-fn select-view-fn})
          (dom/span #js {:className "header-nav-separator"})
          (header-nav-item {:id :history
                            :name "History"
                            :view view
                            :select-fn select-view-fn}))))))

(def header (om/factory Header))
