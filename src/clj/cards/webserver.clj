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
   [:head]
   [:body
    [:div#content]]
   [:script {:src "/cljs.js"}]])

(defroutes app-routes
  (GET "/"
      [] (html main-page))

  (resources "/" {:root "dev"})
  (resources "/" {:root "public"}))

(def app (-> app-routes
             wrap-reload
             handler/site))

;; (defn cljs-repl
;;   []
;;   (cemerick.piggieback/cljs-repl
;;    :repl-env (weasel.repl.websocket/repl-env :ip "0.0.0.0"
;;                                              :port 9001)))

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
