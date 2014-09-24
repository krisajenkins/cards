(ns cards.shadow
  (:require [clojure.core.async :refer [go go-loop timeout alts! <! chan close!]]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [shadow.cljs.build :as shadow]))

(def modules
  [[:cljs '[cljs.core clojure.set clojure.string cljs.core.async cljs.reader] #{}]
   [:devel '[speclj.core weasel.repl cards.devel] #{:cljs}]
   [:cards '[no.en.core inflections.core
             om.core datascript sablono.core
             cards.core] #{:cljs}]])

(defn configure-modules [state modules]
  (reduce (fn [state settings]
            (apply shadow/step-configure-module state settings))
          state
          modules))

(defn initial-build-state
  [source-path config]
  (-> (shadow/init-state)
      (shadow/enable-source-maps)
      (merge config)
      (shadow/step-find-resources-in-jars)
      (shadow/step-find-resources source-path)
      (shadow/step-finalize-config)
      (shadow/step-compile-core)
      (configure-modules modules)))

(defn initial-dev-state
  []
  (initial-build-state "src/cljs"
                       {:optimizations :simple
                        :pretty-print false
                        :work-dir (io/file "target/cljs-work")
                        :public-dir (io/file "resources/dev")
                        :public-path ""}))

(defn initial-prod-state
  []
  (initial-build-state "src/cljs" {:optimizations :advanced
                                   :pretty-print false
                                   :work-dir (io/file "target/cljs-work")
                                   :public-dir (io/file "resources/prod")
                                   :externs ["react/externs/react.js"]}))

(defn dev-build-step
  "Build the project, return the new state."
  [state]
  (try
    (-> state
        (shadow/step-compile-modules)
        (shadow/flush-unoptimized))
    (catch Throwable t
      (prn [:failed-to-compile t])
      (.printStackTrace t)
      state)))

(defn prod-build-step
  "Build the project."
  [state]
  (-> state
      (shadow/step-compile-modules)
      (shadow/closure-optimize)
      (shadow/flush-to-disk)
      (shadow/flush-modules-to-disk)))

(defn rebuild-if-changed!
  "Scans the filesystem for changes, rebuilds if necessary, returns the potentially-modified state."
  [state scan-for-new-files?]
  (let [scan-results (shadow/scan-for-modified-files state scan-for-new-files?)]
    (if (seq scan-results)
      (do (println "Files changed!")
          (dev-build-step (shadow/reload-modified-files! state scan-results)))
      state)))

(defrecord ShadowBuildWatcher
    []
  component/Lifecycle
  (start [component]
    (println "Starting Shadow Build")
    (let [scanning-channel (chan) ; Closing the scanning-channel causes the scanner to stop.
          initial-state (dev-build-step (initial-dev-state))]
      (println "Initial build complete. Watching.")
      (go
        (loop [i 0
               state initial-state]
          (let [t (timeout 200)
                [message port] (alts! [scanning-channel t])]
            (when-not (and (= port scanning-channel)
                           (nil? message))
              (recur (mod (inc i) 10)
                     (rebuild-if-changed! state
                                          (zero? i)))))))

      (assoc component :scanning-channel scanning-channel)))

  (stop [component]
    (println "Stopping Shadow Build")
    (if-let [channel (:scanning-channel component)]
      (close! channel))
    (dissoc component :scanning-channel)))
