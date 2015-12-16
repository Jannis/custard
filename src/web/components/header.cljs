(ns web.components.header
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [web.components.state-chooser :refer [state-chooser]]
            [web.util :refer [delay-call]]))

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
  (init-state [this]
    {:view :nil
     :state nil})

  (select-view [this view]
    (println "Header: select-view" view)
    (om/update-state! this assoc :view view)
    (delay-call 150
                ;; Ensure the view is still selected
                (fn []
                  (= (:view (om/get-state this)) view))
                ;; Select the view
                (fn []
                  (let [{:keys [select-view-fn]} (om/get-computed this)]
                    (when select-view-fn
                      (select-view-fn view))))))

  (select-state [this state]
    (println "Header: select-state" state)
    (let [states (:custard/states (om/props this))
          find-state (fn [name]
                       (first (filter #(= name (:name %)) states)))
          new-state (find-state (second state))]
      (om/update-state! this assoc :state new-state)
      (delay-call 150
                  ;; Select the state
                  (fn []
                    (let [{:keys [select-state-fn]}
                          (om/get-computed this)]
                      (when select-state-fn
                        (select-state-fn state)))))))

  (update-state [this props]
    (let [custard-state (:custard/state props)]
      (om/update-state! this
                        (fn [state]
                          (-> state
                              (assoc :view (:view props))
                              (assoc :state custard-state))))))

  (componentWillMount [this]
    (println "Header: will mount" (:custard/state (om/props this)))
    (.update-state this (om/props this)))

  (componentWillReceiveProps [this new-props]
    (println "Header: will receive props")
    (.update-state this new-props))

  (render [this]
    (let [{:keys [custard/states]} (om/props this)
          {:keys [select-state-fn]} (om/get-computed this)
          {:keys [view state]} (om/get-state this)]
      (println "Header: render" "state" (:name state))
      (dom/header #js {:className "header"}
        (dom/div #js {:className "header-title"}
          (dom/h1 #js {:className "header-title-text"
                       :onClick #(.select-view this :project)}
            (if (and (not (nil? (:project state)))
                     (contains? (:project state) :title))
              (:title (:project state))
              "CUSTARD"))
          (dom/nav #js {:className "header-title-nav"}
            (state-chooser {:state state
                            :states states
                            :select-fn #(.select-state this %)})))
        (dom/nav #js {:className "header-nav"}
          (header-nav-item {:id :requirements
                            :name "Requirements"
                            :view view
                            :select-fn #(.select-view this %)})
          (header-nav-item {:id :components
                            :name "Architecture"
                            :view view
                            :select-fn #(.select-view this %)})
          (header-nav-item {:id :work-items
                            :name "Work Items"
                            :view view
                            :select-fn #(.select-view this %)})
          (header-nav-item {:id :tags
                            :name "Tags"
                            :view view
                            :select-fn #(.select-view this %)})
          (dom/span #js {:className "header-nav-separator"})
          (header-nav-item {:id :history
                            :name "History"
                            :view view
                            :select-fn #(.select-view this %)}))))))

(def header (om/factory Header))
