(ns cards.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan <! >! put!]]
            [datascript :as ds]
            [cards.render :as render]
            [cards.ui-messages :refer [process-message]]))

(def schema
  {:aka {:db/cardinality :db.cardinality/many}})

(defn initial-db
  []
  (doto (ds/create-conn schema)
    (ds/transact! [{:db/id -1
                    :type :person
                    :name  "Kris Jenkins"
                    :age 36
                    :aka ["The Jenkster" "Papa"]}
                   {:db/id -2
                    :type :person
                    :name "Cazabon Jenkins"
                    :age 3
                    :aka ["Cazington Bear" "The Dude"]}])))

(def app {:db (initial-db)})

(defn ^:export main
  []
  (let [event-loop (render/setup (:db app)
                                 "#content")]
    (go-loop []
      (let [message (<! event-loop)]
        (js/console.log "Received message" (pr-str message))
        (process-message message (:db app))
        (recur)))))
