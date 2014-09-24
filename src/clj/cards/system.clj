(ns cards.system
  (:require [com.stuartsierra.component :as component]
            [cards.shadow :as shadow]
            [cards.webserver :as ws]))

(defn cards-dev-system
  [{:keys [webserver-port]
    :as config-options}]
  (component/system-map
   :webserver (ws/map->Webserver {:port webserver-port})
   :shadowbuild (shadow/->ShadowBuildWatcher)))
