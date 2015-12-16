(ns web.components.nodes
  (:require [clojure.string :as str]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [web.components.markdown :refer [markdown]]
            [web.components.node-link :refer [NodeLink node-link
                                              node->route]]
            [web.routing :as routing]))

(defn sort-nodes [nodes]
  (letfn [(kind-index [node]
            (.indexOf #js ["requirement"
                           "component"
                           "work-item"
                           "tag"]
                      (:kind node)))]
    (sort-by (juxt kind-index :title) nodes)))

(declare node)

(defui Node
  static om/Ident
  (ident [this props]
    [:node (:name props)])
  static om/IQuery
  (query [this]
    [:name :title :kind :description :marker
     {:parent (om/get-query NodeLink)}
     {:children '...}
     {:mapped-here (om/get-query NodeLink)}
     {:mapped-to (om/get-query NodeLink)}
     {:tags (om/get-query NodeLink)}
     {:tagged (om/get-query NodeLink)}
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

  (markers [this]
    (let [{:keys [tags]} (om/props this)]
      (into [] (distinct (keep identity (map :marker tags))))))

  (render-detail [this label content]
    (dom/div #js {:className "node-detail"}
      (dom/div #js {:className "node-detail-label"} label)
      (dom/div #js {:className "node-detail-content"} content)))

  (render [this]
    (let [{:keys [name title kind description children
                  mapped-to mapped-here tags tagged
                  ui/expanded]} (om/props this)
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
            (str title " ")
            (dom/span #js {:className "node-header-title-markers"}
              (str/join " " (.markers this))))
          (dom/a #js {:className "node-header-name"
                      :onClick #(.set-permalink this)}
            name))
        (dom/div #js {:className
                      (str "node-details"
                           (if expanded
                             " node-details-expanded"
                             " node-details-collapsed"))}
          (when (not (empty? description))
            (let [text {:text description}]
              (dom/div #js {:className "node-description"}
                (dom/h3 nil "Description")
                (markdown text))))
          (dom/div #js {:className "node-details-table"}
            (when parent
              (.render-detail this "Parent" (node-link parent)))
            (when (some #{kind} ["component" "work-item"])
              (.render-detail this "Mapped here"
                (if (empty? mapped-here)
                  (dom/div #js {:className "error"}
                    (if (= "component" kind)
                      "No requirements have been mapped here yet."
                      (str "No requirements or components have been "
                           "mapped here yet.")))
                  (map node-link (sort-nodes mapped-here)))))
            (when (some #{kind} ["requirement" "component"])
              (.render-detail this "Mapped to"
                (if (empty? mapped-to)
                  (dom/div #js {:className "error"}
                    (if (= "requirement" kind)
                      (str "Not mapped to any components or "
                           "work items yet.")
                      (str "Not mapped to any work items yet.")))
                  (map node-link (sort-nodes mapped-to)))))
            (when-not (or (= "tag" kind) (empty? tags))
              (.render-detail this "Tags" (mapv node-link tags)))
            (when (= "tag" kind)
              (.render-detail this "Tagged"
                (if (empty? tagged)
                  "Nothing has been tagged with this yet."
                  (map node-link (sort-nodes tagged)))))))
        (dom/div #js {:className "node-subnodes"}
          (for [child (sort-nodes children)]
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
