(ns cards.webserver
  (:require [org.httpkit.server :refer [run-server]]
            [com.stuartsierra.component :as component]
            [compojure.route :refer [resources]]
            [compojure.handler :as handler]
            [compojure.core :refer :all]
            [hiccup.core :refer [html]]
            [ring.middleware.reload :refer [wrap-reload]]))

(def main-page
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
                 "/devel.js"
                 "/cards.js"]]
     [:script {:src script}])])

(defroutes app-routes
  (GET "/"
      [] (html main-page))

  (resources "/" {:root "META-INF/resources/webjars/bootstrap/3.2.0/"})
  (resources "/" {:root "react"})
  (resources "/" {:root "dev"})
  (resources "/" {:root "public"}))

(def app (-> app-routes
             wrap-reload
             handler/site))

(defrecord Webserver [port shutdown-fn]
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
