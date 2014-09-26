(ns cards.system
  (:require [com.stuartsierra.component :as component]
            [cards.shadow :as shadow]
            [cards.webserver :as ws]))

(defn cards-dev-system
  [{:keys [webserver-port build-type]
    :as config-options}]
  (component/system-map
   :shadowbuild (shadow/->ShadowBuildWatcher build-type)
   :webserver (component/using
               (ws/map->Webserver {:port webserver-port})
               [:shadowbuild])))
