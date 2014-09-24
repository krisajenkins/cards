(ns cards.render
  (:require [cljs.core.async :refer [chan <! >! put!]]
            [cljs.reader :as edn]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [datascript :as ds]
            [cards.ui-messages :as msg]))

(defn state-event-handler
  ([owner korks]
     (state-event-handler owner korks identity))
  ([owner korks f]
     (fn [event]
       (om/set-state! owner korks
                      (f (.. event -target -value))))))

(defn message-handler
  [owner message]
  (fn [event]
    (let [event-channel (om/get-shared owner :event-channel)]
      (put! event-channel
            message))
    (.stopPropagation event)
    (.preventDefault event)))

(defn query-people
  [connection]
  (ds/q '{:find [?name ?age]
          :where [[?e :type :person]
                  [?e :name ?name]
                  [?e :age ?age]]}
        connection))

(defn person-row
  [[name age] owner]
  (reify
    om/IRender
    (render [this]
      (html [:tr
             [:td.name name]
             [:td.age age]]))))

(defn people-view
  [connection owner]
  (reify
    om/IRender
    (render [this]
      (html [:div
             [:h1 "People"]
             [:div.col-md-4
              [:table.table.table-condensed
               [:thead]
               [:tbody
                (om/build-all person-row
                              (query-people (om/value connection)))]]]]))))

(defn person-form
  [connection owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [name age]
                      :as state}]
      (html [:form.form.col-md-4 {:role "form"}
             [:code (pr-str state)]
             [:div.form-group
              [:input.form-control {:type :text
                                    :on-change (state-event-handler owner :name)
                                    :placeholder "Name"
                                    :value name}]]

             [:div.form-group
              [:input.form-control {:type :number
                                    :on-change (state-event-handler owner :age edn/read-string)
                                    :placeholder "Age"
                                    :value age}]]
             [:p
              [:button.btn.btn-info.btn-block {:on-click (message-handler owner
                                                                          (msg/map->AddPerson {:name name
                                                                                               :age age}))}
               "Add"]]]))))

(defn app-view [connection owner]
  (reify
    om/IRender
    (render [this]
      (html [:div.container
             [:div.row
              (om/build people-view connection)]
             [:div.row
              (om/build person-form connection)]]))))

(defn setup
  "Sets up the Om render loop, returns an event channel."
  [db selector]
  (let [event-channel (chan 5)]
    (om/root app-view
             db
             {:target (js/document.querySelector selector)
              :shared {:event-channel event-channel}})
    event-channel))
