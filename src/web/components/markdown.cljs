(ns web.components.markdown
  (:require [cljsjs.showdown]
            [goog.crypt.base64 :as base64]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(defn uml-extension [converter]
  #js [#js {:type "lang"
            :regex "(@startuml([^]*?)@enduml)"
            :replace
            (fn [s match]
              (let [data (base64/encodeString match)
                    url (str "http://localhost:3001/uml/" data)]
                (str "[![UML](" url ")](" url ")")))}])

(def converter
  (js/Showdown.converter. #js {:extensions #js [uml-extension]}))

(defui Markdown
  Object
  (render [this]
    (let [text (:text (om/props this))
          html (.makeHtml converter text)]
      (dom/div #js {:className "markdown"
                    :dangerouslySetInnerHTML #js {:__html html}}))))

(def markdown (om/factory Markdown))
