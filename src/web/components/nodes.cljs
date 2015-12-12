(ns web.components.nodes
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [web.components.markdown :refer [markdown]]
            [web.components.node-link :refer [NodeLink node-link
                                              node->route]]
            [web.routing :as routing]))

(declare node)

(defui Node
  static om/Ident
  (ident [this props]
    [:node (:name props)])
  static om/IQuery
  (query [this]
    [:name :title :kind :description
     {:parent (om/get-query NodeLink)}
     {:children '...}
     {:mapped-here (om/get-query NodeLink)}
     {:mapped-to (om/get-query NodeLink)}
     :ui/expanded])
  Object
  (toggle-expanded [this]
    (let [ident (om/get-ident this)]
      (om/transact! this `[(app/toggle-node-expanded {:node ~ident})])))

  (set-permalink [this]
    (let [route (node->route (om/props this))]
      (routing/activate-route! route)))

  (satisfied? [this]
    (let [{:keys [kind mapped-here mapped-to]} (om/props this)]
      (case kind
        "requirement" (not (empty? mapped-to))
        "component" (and (not (empty? mapped-here))
                         (not (empty? mapped-to)))
        "work-item" (not (empty? mapped-here))
        "tag" true)))

  (render [this]
    (let [{:keys [name title kind description children
                  mapped-to mapped-here ui/expanded]} (om/props this)
          {:keys [parent]} (om/get-computed this)]
      (dom/div #js {:className
                    (str "node"
                         (when-not parent
                           " node-root")
                         (if (.satisfied? this)
                           " node-satisfied"
                           " node-unsatisfied"))}
        (dom/h2 #js {:className "node-header"}
          (dom/span #js {:className "node-header-title"
                         :onClick #(.toggle-expanded this)}
            title)
          (dom/a #js {:className "node-header-name"
                      :onClick #(.set-permalink this)}
            name))
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
            (when-not (= kind "requirement")
              (dom/div #js {:className "node-detail"}
                (dom/div #js {:className "node-detail-label"}
                  "Mapped here")
                (dom/div #js {:className "node-detail-content"}
                  (if (empty? mapped-here)
                    (dom/div #js {:className "error"}
                      "No requirements have been mapped here yet.")
                    (for [source mapped-here]
                      (node-link source))))))
            (when-not (some #{kind} ["work-item" "tag"])
              (dom/div #js {:className "node-detail"}
                (dom/div #js {:className "node-detail-label"}
                  "Mapped to")
                (dom/div #js {:className "node-detail-content"}
                  (if (empty? mapped-to)
                    (dom/div #js {:className "error"}
                      "Not mapped to any components yet.")
                    (for [target mapped-to]
                      (node-link target))))))))
        (dom/div #js {:className "node-subnodes"}
          (for [child children]
            (node
              (om/computed child {:parent (om/props this)}))))))))

(def node (om/factory Node {:key-fn :name}))

(defui Nodes
  Object
  (render [this]
    (let [nodes (:nodes (om/props this))]
      (dom/div #js {:className "nodes"}
        (for [node' (filter #(nil? (:parent %)) nodes)]
          (node node'))))))

(def nodes (om/factory Nodes))
