(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [com.stuartsierra.component :as component]
            [cards.system :refer [cards-system]]
            [cemerick.piggieback :as piggieback]
            [weasel.repl.websocket :refer [repl-env]]))

(def system
  "A Var containing an object representing the application under
  development."
  (atom (cards-system {:build-type :dev
                       :webserver-port 8000})))

(defn start
  "Initializes and starts the system running."
  []
  (swap! system component/start)
  :ready)

(defn stop
  "Shuts down the system."
  []
  (swap! system component/stop)
  :done)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (time
   (do
     (swap! system component/stop)
     (refresh :after 'user/start))))

(defn cljs
  ([]
     (cljs 9001))
  ([port]
     (piggieback/cljs-repl :repl-env (repl-env :ip "0.0.0.0"
                                               :port port))))
