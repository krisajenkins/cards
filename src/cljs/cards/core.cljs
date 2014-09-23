(ns cards.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [chan <! >!]]
            ;; [goog.dom]
            ;; [sablono.core :refer-macros [html]]
            ;; [weasel.repl :as ws-repl]
            ;; [om.core :as om :include-macros true]
            ;; [datascript :as ds]
            )
  )

;; (ws-repl/connect "ws://localhost:9001" :verbose true)

;; (def db-conn
;;   (doto (ds/create-conn {:aka {:db/cardinality :db.cardinality/many}})
;;     (ds/transact! [{:db/id -1
;;                     :name  "Maksim"
;;                     :age   45
;;                     :aka   ["Maks Otto von Stirlitz", "Jack Ryan"]}])))

;; (defn widget [connection owner]
;;   (reify
;;     om/IRender
;;     (render [this]
;;       (html [:code (pr-str (ds/q '[:find ?n ?a
;;                                    :where [?e :aka "Maks Otto von Stirlitz"]
;;                                    [?e :name ?n]
;;                                    [?e :age  ?a]]
;;                                  connection))]))))

;; (om/root widget
;;          db-conn
;;          {:target (goog.dom/getElement "#content")
;;           :shared {:event-channel (chan 5)}})

(js/console.log "Hello world!")
