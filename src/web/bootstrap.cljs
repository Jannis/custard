(ns web.bootstrap
  (:require-macros [web.bootstrap :as bootstrap])
  (:require [cljsjs.react-bootstrap]
            [om.next :as om]))

(defn force-children [x]
  (cond->> x
    (seq? x) (into [] (map force-children))))

(bootstrap/gen-react-bootstrap-fns)
