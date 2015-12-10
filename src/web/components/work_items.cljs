(ns web.components.work-items
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [web.components.markdown :refer [markdown]]
            [web.components.node-link :refer [NodeLink node-link]]))

(declare work-item)

(defui WorkItem
  static om/Ident
  (ident [this props]
    [:node (:name props)])
  static om/IQuery
  (query [this]
    [:name :title :kind :description
     {:parent (om/get-query NodeLink)}
     {:children '...}
     {:mapped-to (om/get-query NodeLink)}
     {:mapped-here (om/get-query NodeLink)}])
  Object
  (toggle-expanded [this]
    (om/update-state! this update :expanded not))

  (render [this]
    (let [{:keys [name title description children
                  mapped-to mapped-here]} (om/props this)
          {:keys [parent]} (om/get-computed this)
          {:keys [expanded]} (om/get-state this)]
      (dom/div #js {:className
                    (str "node"
                         (when-not parent
                           " node-root")
                         (if-not (empty? mapped-here)
                           " node-satisfied"
                           " node-unsatisfied"))}
        (dom/h2 #js {:className "node-header"
                     :onClick #(.toggle-expanded this)}
          (dom/span #js {:className "node-header-title"} title)
          (dom/span #js {:className "node-header-name"} name))
        (dom/div #js {:className
                      (str "node-details"
                           (if expanded
                             " node-details-expanded"
                             " node-details-collapsed"))}
          (dom/div #js {:className "node-details-table"}
            (when description
              (dom/div #js {:className "node-detail"}
                (dom/div #js {:className "node-detail-label"}
                  "Description")
                (dom/div #js {:className "node-detail-content"}
                  (markdown {:text description}))))
            (when parent
              (dom/div #js {:className "node-detail"}
                (dom/div #js {:className "node-detail-label"}
                  "Parent")
                (dom/div #js {:className "node-detail-content"}
                         (node-link parent))))
            (dom/div #js {:className "node-detail"}
              (dom/div #js {:className "node-detail-label"}
                "Mapped here")
              (dom/div #js {:className "node-detail-content"}
                (if (empty? mapped-here)
                  (dom/div #js {:className "error"}
                    (str "No requirements and components have been "
                         "mapped here yet."))
                  (for [source mapped-here]
                    (node-link source)))))))
        (dom/div #js {:className "node-subnodes"}
          (for [child children]
            (work-item
              (om/computed child {:parent (om/props this)}))))))))

(def work-item (om/factory WorkItem {:key-fn :name}))

(defui WorkItems
  Object
  (render [this]
    (let [items (:work-items (om/props this))]
      (dom/div #js {:className "nodes"}
        (for [item (filter #(nil? (:parent %)) items)]
          (work-item item))))))

(def work-items (om/factory WorkItems))
