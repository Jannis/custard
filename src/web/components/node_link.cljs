(ns web.components.node-link
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [web.routing :as routing]))

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

(def kind->handler
  {"requirement" :expanded-requirements
   "component" :expanded-components
   "work-item" :expanded-work-items
   "tag" :expanded-tags})

(defn node->route [{:keys [kind name]}]
  {:handler (kind->handler kind)
   :route-params {:state '?state
                  :node name}})

(defui NodeLink
  static om/Ident
  (ident [this props]
    [:node (:name props)])
  static om/IQuery
  (query [this]
    [:name :title :kind])
  Object
  (render [this]
    (let [{:keys [name title kind]} (om/props this)
          route (node->route (om/props this))
          invalid? (not kind)]
      (if invalid?
        (dom/span #js {:className "error"}
          (str "Invalid reference – " name))
        (dom/a #js {:onClick #(routing/activate-route! route)
                    :className (str "node-link")}
               (badge {:kind kind})
               (dom/span #js {:className "node-link-title"} title)
               (dom/span #js {:className "node-link-name"} name))))))

(def node-link (om/factory NodeLink {:key-fn :name}))
