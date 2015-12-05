(ns web.app
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [web.bootstrap :refer [nav
                                   nav-item
                                   nav-brand
                                   navbar
                                   panel-group
                                   tab
                                   tabs]]
            [web.reconciler :refer [reconciler]]
            [web.components.requirement :refer [Requirement
                                                requirement]]))

(enable-console-print!)

(defui App
  static om/IQueryParams
  (params [this]
    {:state "UNCOMMITTED"})
  static om/IQuery
  (query [this]
    `[({:requirements ~(om/get-query Requirement)} {:state ?state})])
  Object
  (render [this]
    (dom/div #js {:className "app"}
      (dom/header nil
        (navbar #js {:inverse true}
          (nav-brand nil "CUSTARD")
          (nav #js {:pullRight true}
            (nav-item #js {:eventKey 1}
                      "Select branch"))))
      (dom/main nil
        (tabs #js {:justified true
                   :defaultActiveKey 1}
          (tab #js {:title "Requirements"
                    :eventKey 1}
            (panel-group nil
              (for [r (:requirements (om/props this))]
                (requirement r))))
          (tab #js {:title "Architecture"
                    :eventKey 2}
            (panel-group nil))
          (tab #js {:title "Work Items"
                    :eventKey 3}
            (panel-group nil)))))))

(defn run []
  (enable-console-print!)
  (om/add-root! reconciler App (gdom/getElement "app")))
