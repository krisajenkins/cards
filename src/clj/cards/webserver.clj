(ns cards.webserver
  (:require [clojure.java.io :as io]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :refer [resources]]
            [com.stuartsierra.component :as component]
            [hiccup.core :refer [html]]
            [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.strategies :refer [serve-live-assets]]
            [optimus.prime :as optimus]
            [optimus.hiccup :refer [link-to-css-bundles link-to-js-bundles]]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [stasis.core :as stasis]))

(def index-page
  [:html {:lang "en"}
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, minimal-ui"}]
    [:link {:rel "stylesheet" :href "/css/bootstrap.css"}]]

   [:body
    [:div#content]]

   (for [script ["/react.js"
                 "/cljs.js"
                 "/cards.js"
                 "/devel.js"]]
     [:script {:src script}])])

(defn files-from-resource
  [directory files]
  (zipmap files
          (map #(slurp (io/resource (str directory %)))
               files)))

(def pages
  (stasis/merge-page-sources
   {:dev (stasis/slurp-directory "resources/dev/" #"\.(js|map)$")
    :public (stasis/slurp-directory "resources/public" #".*\..*")
    :react (files-from-resource "react" ["/react.js"])
    :bootstrap (files-from-resource "META-INF/resources/webjars/bootstrap/3.2.0"
                                    ["/css/bootstrap.css"
                                     "/css/bootstrap.css.map"
                                     "/fonts/glyphicons-halflings-regular.woff"
                                     "/fonts/glyphicons-halflings-regular.ttf"
                                     "/fonts/glyphicons-halflings-regular.svg"])
    :generated {"/index.html" (html index-page)}}))

((defn export
   []
   (let [target "target/site"]
     (stasis/empty-directory! target)
     (stasis/export-pages pages target))))

(def app
  (-> (stasis/serve-pages pages)
      wrap-reload
      handler/site))

(defrecord Webserver
    [port shutdown-fn]

  component/Lifecycle
  (start [component]
    (println "Starting webserver.")
    (assoc component :shutdown-fn (run-server app
                                              {:port port
                                               :join? false})))

  (stop [{:keys [shutdown-fn]
          :as component}]
    (println "Stopping webserver.")
    (when shutdown-fn
      (shutdown-fn :timeout 100))
    (assoc component :shutdown-fn nil)))
