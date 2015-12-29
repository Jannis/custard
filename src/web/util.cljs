(ns web.util
  (:require [cljs.core.async :refer [<! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn delay-call
  ([ms f]
   (delay-call ms f (constantly true)))
  ([ms pred f]
   (go
     (<! (timeout ms))
     (when (pred)
       (f)))))
