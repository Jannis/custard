#!/usr/bin/env boot

(set-env!
 :source-paths #{"src" "test"}
 :resource-paths #{"resources"}
 :dependencies '[;; Boot
                 [adzerk/boot-cljs "1.7.170-1"]
                 [adzerk/boot-cljs-repl "0.3.0"]
                 [adzerk/boot-reload "0.4.1"]
                 [danielsz/boot-environ "0.0.5"]
                 [deraen/boot-less "0.4.2"]

                 ;; REPL
                 [com.cemerick/piggieback "0.2.1"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [weasel "0.7.0"]

                 ;; General
                 [adzerk/env "0.2.0"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]

                 ;; CUSTARD
                 [com.stuartsierra/component "0.3.1"]
                 [gitiom "0.1.1"]
                 [juxt/dirwatch "0.2.2"]
                 [me.raynes/fs "1.4.6"]
                 [org.clojars.mpenet/clj-yaml "0.3.4"]

                 ;; Server
                 [com.cognitect/transit-clj "0.8.285"]
                 [compojure "1.4.0"]
                 [environ "1.0.1"]
                 [http-kit "2.1.19"]
                 [net.sourceforge.plantuml/plantuml "8033"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/tools.reader "1.0.0-alpha1"]
                 [org.danielsz/system "0.1.9"]
                 [ring-middleware-format "0.6.0"]

                 ;; Web app
                 [com.cognitect/transit-cljs "0.8.232"]
                 [org.clojure/core.async "0.2.374"]
                 [org.omcljs/om "1.0.0-alpha26"]
                 [cljsjs/showdown "0.4.0-1"]])

(task-options!
 pom {:project 'custard
      :version "0.1.0-SNAPSHOT"})

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[adzerk.boot-reload :refer [reload]]
         '[danielsz.boot-environ :refer [environ]]
         '[deraen.boot-less :refer [less]]
         '[reloaded.repl :refer [init start stop go reset]]
         '[system.boot :refer [run system]]
         '[server.systems :refer [development-system
                                  production-system]])

(deftask run-development
  []
  (comp (watch)
        (system :sys #'development-system
                :auto-start true
                :hot-reload true
                :files ["core.clj" "handler.clj" "parser.clj"])
        (reload :on-jsload 'web.app/run)
        (cljs-repl)
        (less)
        (cljs :source-map true
              :optimizations :none)
        (repl :server true)))

(deftask build-production
  []
  (comp (less)
        (cljs :optimizations :simple)
        (aot :namespace '#{server.core})))

(deftask run-production
  []
  (comp (build-production)
        (run :main-namespace "server.core"
             :arguments [#'production-system])
        (wait)))

(deftask uberjar
  []
  (comp (build-production)
        (pom)
        (uber)
        (jar :main 'server.core)))
