(defproject cards "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/core.match "0.2.2"]
                 [prismatic/schema "0.2.6"]

                 ;; ClojureScript
                 [org.clojure/clojurescript "0.0-2342"]
                 [thheller/shadow-build "0.9.3"]
                 [org.webjars/bootstrap "3.2.0"]
                 [om "0.7.3"]
                 [noencore "0.1.16"]
                 [sablono "0.2.22"]
                 [datascript "0.4.1"]]
  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test/clj" "test/cljs"]
  :plugins []
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[com.stuartsierra/component "0.2.2"]
                                  [org.clojure/tools.namespace "0.2.5"]

                                  [speclj "3.1.0"]

                                  [compojure "1.1.9" :exclusions [ring/ring-core]]
                                  [ring "1.3.1"]
                                  [http-kit "2.1.19"]
                                  [hiccup "1.0.5"]

                                  [com.cemerick/piggieback "0.1.3"]
                                  [weasel "0.4.0-SNAPSHOT"]

                                  [optimus "0.15.0"]
                                  [stasis "2.2.1" :exclusions [org.clojure/clojure]]]
                   :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}})
