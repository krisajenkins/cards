(ns cards.webserver
  (:require [clojure.java.io :as io]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :refer [resources]]
            [com.stuartsierra.component :as component]
            [hiccup.core :refer [html]]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.strategies :as strategies]
            [optimus.prime :as optimus]
            [optimus.export :as export]
            [optimus.link :as link]
            [optimus.hiccup :refer [link-to-css-bundles link-to-js-bundles]]
            [stasis.core :as stasis]))

(defn index-page
  [request]
  (html [:html {:lang "en"}
         [:head
          [:meta {:charset "utf-8"}]
          [:meta {:name "viewport"
                  :content "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, minimal-ui"}]

          (link-to-css-bundles request ["/css/bootstrap.css"])]

         [:body [:div#content]]

         (link-to-js-bundles request ["/react.js" "/cljs.js" "/cards.js"])

         (when (not= (:build-type request)
                     :prod)
           (link-to-js-bundles request ["/devel.js" "/test.js"]))

         [:script "cards.core.main()"]]))

(defn get-pages
  []
  (stasis/merge-page-sources
   {:generated {"/index.html" index-page}}))

(defn get-assets
  []
  (concat (assets/load-assets "public" [#".+"])
          (assets/load-assets "out" [#"/.+\.js$"
                                     #"/.+\.js.map$"])
          (assets/load-bundles "META-INF/resources/webjars/bootstrap/3.2.0"
                               {"/css/bootstrap.css" ["/css/bootstrap.css"]
                                "/css/bootstrap.css.map" ["/css/bootstrap.css.map"]})
          (assets/load-bundles "react" {"/react.js" ["/react.min.js"]})
          (assets/load-bundles "out" {"/cljs.js" ["/cljs.js"]
                                      "/cards.js" ["/cards.js"]
                                      "/devel.js" ["/devel.js"]
                                      "/test.js" ["/test.js"]})))

(defn export-site!
  [build-type]
  (time
   (let [target-dir "target/site"
         assets (as-> (get-assets) $
                      (if (= build-type :prod)
                        (optimizations/all $ {})
                        (optimizations/none $ {}))
                      (remove :bundled $)
                      (remove :outdated $))
         request {:optimus-assets assets
                  :build-type build-type}]
     (stasis/empty-directory! target-dir)
     (optimus.export/save-assets assets target-dir)
     (stasis/export-pages (get-pages) target-dir request))))

(defn app
  []
  (-> (stasis/serve-pages get-pages)
      (optimus/wrap get-assets optimizations/none strategies/serve-live-assets)
      wrap-reload
      handler/site))

(defrecord Webserver
    [port shutdown-fn]

  component/Lifecycle
  (start [component]
    (println "Starting webserver.")
    (assoc component :shutdown-fn (run-server (app)
                                              {:port port
                                               :join? false})))

  (stop [{:keys [shutdown-fn]
          :as component}]
    (println "Stopping webserver.")
    (when shutdown-fn
      (shutdown-fn :timeout 100))
    (assoc component :shutdown-fn nil)))

(defrecord Exporter
    [build-type]
  component/Lifecycle
  (start [component]
    (println "Starting Exporter")
    (export-site! (:build-type component))
    component)
  (stop [component]
    (println "Stopping Exporter")
    (assoc component
      :build-type nil)))
