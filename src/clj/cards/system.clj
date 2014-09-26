(ns cards.system
  (:require [com.stuartsierra.component :as component]
            [cards.shadow :as shadow]
            [cards.webserver :as ws]))

(defmulti cards-system
  (fn [config] (:build-type config)))

(defmethod cards-system :dev
  [{:keys [build-type webserver-port]
    :as config}]
  (component/system-map
   :shadowbuild (shadow/->ShadowBuildWatcher build-type)
   :webserver (component/using (ws/map->Webserver {:port webserver-port})
                               [:shadowbuild])))

(defmethod cards-system :prod
  [{:keys [build-type webserver-port]
    :as config}]
  (component/system-map
   :shadowbuild (shadow/->ShadowBuildWatcher build-type)
   :exporter (component/using (ws/->Exporter build-type)
                              [:shadowbuild])))


(defn -main
  []
  (let [system (atom (cards-system {:build-type :prod}))]
    (swap! system component/start)
    (swap! system component/stop)))
