(ns web.components.markdown
  (:require [markdown.core :as markdown]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(defui Markdown
  Object
  (render [this]
    (let [text (:text (om/props this))
          html (markdown/md->html text)]
      (dom/div #js {:className "markdown"
                    :dangerouslySetInnerHTML #js {:__html html}}))))

(def markdown (om/factory Markdown))
