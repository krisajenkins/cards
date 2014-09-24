(ns cards.ui-messages
  (:require [datascript :as ds]))

(defprotocol UIMessage
  (process-message [this db]))

(defn add-person
  [db data]
  (ds/transact! db (merge {:db/id -1
                           :type :person}
                          data)))

(defrecord AddPerson
    [name age]
  UIMessage
  (process-message [this db]
    (add-person db this)))
