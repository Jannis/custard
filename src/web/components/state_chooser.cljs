(ns web.components.state-chooser
  (:require [clojure.string :as str]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(defui StateChooserItem
  static om/Ident
  (ident [this props]
    [:state (:name props)])
  static om/IQuery
  (query [this]
    [:name :revision :type])
  Object
  (render [this]
    (let [{:keys [name type]} (om/props this)
          prefixes #"refs/heads/|refs/remotes/|refs/tags/"]
      (dom/option #js {:value name}
        (condp = type :branch "b " :tag "t " " ")
        (str/replace name prefixes "")))))

(def state-chooser-item (om/factory StateChooserItem {:keyfn :name}))

(defui StateChooser
  Object
  (state-selected [this e]
    (let [id (.. e -target -value)
          {:keys [select-fn]} (om/props this)]
      (when select-fn
        (select-fn [:state id])))
    (.preventDefault e))

  (render [this]
    (let [{:keys [state states]} (om/props this)]
      (dom/div #js {:className "state-chooser"}
        (dom/select #js {:value (:name state)
                         :onChange #(.state-selected this %)}
          (for [state' states]
            (state-chooser-item state')))))))

(def state-chooser (om/factory StateChooser))
