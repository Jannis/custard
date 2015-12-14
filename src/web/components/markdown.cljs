(ns web.components.markdown
  (:require [cljsjs.showdown]
            [clojure.string :as str]
            [goog.crypt.base64 :as base64]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [web.env :as env]
            [web.reconciler :refer [get-current-state]]))

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

(defn file-extension [converter]
  #js [#js {:type "lang"
            :regex "\\[(.+)\\]\\{(.+)\\}(@([^\\s]+))?"
            :replace
            (fn [s title path _ state]
              (let [state (if state state (second (get-current-state)))
                    state' (str/replace state #"/" ":")
                    url (str/join "/" [env/BACKEND_URL "file"
                                       state' path])]
                (str "[" title "](" url " \"" path "\")")))}])

(defn tag-extension [converter]
  #js [#js {:type "lang"
            :regex "(\\+[a-zA-Z0-9-_\\/:]+)"
            :replace
            (fn [s name]
              (str "<span class=\"inline-tag\">" name "</span>"))}])

(def converter
  (let [extensions #js [uml-extension file-extension tag-extension]]
    (js/Showdown.converter. #js {:extensions extensions})))

(defui Markdown
  Object
  (render [this]
    (let [text (:text (om/props this))
          html (.makeHtml converter text)]
      (dom/div #js {:className "markdown"
                    :dangerouslySetInnerHTML #js {:__html html}}))))

(def markdown (om/factory Markdown))
