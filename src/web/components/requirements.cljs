(ns web.components.requirements
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(defui Parent
  static om/Ident
  (ident [this props]
    [:node (:name props)])
  static om/IQuery
  (query [this]
    [:name :title])
  Object
  (render [this]
    (dom/div nil
      (:title (om/props this)))))

(def parent* (om/factory Parent))

(declare requirement)

(defui Requirement
  static om/Ident
  (ident [this props]
    [:node (:name props)])
  static om/IQuery
  (query [this]
    [:name :title :description
     {:parent (om/get-query Parent)}
     {:children '...}])
  Object
  (toggle-expanded [this]
    (om/update-state! this update :expanded not))

  (render [this]
    (let [{:keys [name title description children]} (om/props this)
          {:keys [parent]} (om/get-computed this)
          {:keys [expanded]} (om/get-state this)]
      (dom/div #js {:className
                    (str "node"
                         (when-not parent
                           " node-root"))}
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
                  description)))
            (when parent
              (dom/div #js {:className "node-detail"}
                (dom/div #js {:className "node-detail-label"}
                  "Parent")
                (dom/div #js {:className "node-detail-content"}
                  (parent* parent))))))
        (dom/div #js {:className "node-subnodes"}
          (for [child children]
            (requirement
              (om/computed child {:parent (om/props this)}))))))))

(def requirement (om/factory Requirement {:key-fn :name}))

(defui Requirements
  Object
  (render [this]
    (println (om/props this))
    (let [items (:requirements (om/props this))]
      (dom/div #js {:className "nodes"}
        (for [item (filter #(nil? (:parent %)) items)]
          (requirement item))))))

(def requirements (om/factory Requirements))
