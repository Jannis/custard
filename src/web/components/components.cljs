(ns web.components.components
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

(defui MapTarget
  static om/Ident
  (ident [this props]
    [:node (:name props)])
  static om/IQuery
  (query [this]
    [:name :title :kind])
  Object
  (render [this]
    (let [{:keys [name title kind]} (om/props this)]
      (dom/div #js {:className "node-map-target"}
               title))))

(def map-target (om/factory MapTarget {:key-fn :name}))

(declare component)

(defui Component
  static om/Ident
  (ident [this props]
    [:node (:name props)])
  static om/IQuery
  (query [this]
    [:name :title :description
     {:parent (om/get-query Parent)}
     {:children '...}
     {:mapped-to (om/get-query MapTarget)}])
  Object
  (toggle-expanded [this]
    (om/update-state! this update :expanded not))

  (render [this]
    (let [{:keys [name title description children
                  mapped-to]} (om/props this)
          {:keys [parent]} (om/get-computed this)
          {:keys [expanded]} (om/get-state this)]
      (dom/div #js {:className
                    (str "node"
                         (when-not parent
                           " node-root")
                         (if-not (empty? mapped-to)
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
                  description)))
            (when parent
              (dom/div #js {:className "node-detail"}
                (dom/div #js {:className "node-detail-label"}
                  "Parent")
                (dom/div #js {:className "node-detail-content"}
                  (parent* parent))))
            (dom/div #js {:className "node-detail"}
              (dom/div #js {:className "node-detail-label"}
                "Mapped to")
              (dom/div #js {:className "node-detail-content"}
                (if (empty? mapped-to)
                  (dom/div #js {:className "error"}
                    "Not mapped to any work items yet.")
                  (for [target mapped-to]
                    (map-target target)))))))
        (dom/div #js {:className "node-subnodes"}
          (for [child children]
            (component
              (om/computed child {:parent (om/props this)}))))))))

(def component (om/factory Component {:key-fn :name}))

(defui Components
  Object
  (render [this]
    (println (om/props this))
    (let [items (:components (om/props this))]
      (dom/div #js {:className "nodes"}
        (for [item (filter #(nil? (:parent %)) items)]
          (component item))))))

(def components (om/factory Components))
