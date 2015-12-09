(ns web.components.markdown
  (:require [cljsjs.showdown]
            [clojure.string :as str]
            [goog.crypt.base64 :as base64]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [web.env :as env]))

(defn base64-encode [data]
  (str/replace (base64/encodeString data)
               #"\+|/"
               {"+" "-"
                "/" "_"}))

(defn uml-extension [converter]
  #js [#js {:type "lang"
            :regex "(@startuml([^]*?)@enduml)"
            :replace
            (fn [s match]
              (let [data (base64-encode match)
                    url (str/join "/" [env/BACKEND_URL "uml" data])]
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
