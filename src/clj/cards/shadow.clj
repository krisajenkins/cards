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

(def build-config
  {:dev {:resource-paths ["src/cljs"]
         :optimizations :simple
         :pretty-print false
         :work-dir (io/file "target/cljs-work")
         :public-dir (io/file "resources/dev")
         :public-path ""}
   :prod {:resource-paths ["src/cljs"]
          :optimizations :advanced
          :pretty-print false
          :work-dir (io/file "target/cljs-work")
          :public-dir (io/file "resources/prod")
          :externs ["react/externs/react.js"]}})

(defn configure-modules [state modules]
  (reduce (fn [state settings]
            (apply shadow/step-configure-module state settings))
          state
          modules))

(defn find-sources
  [{:keys [resource-paths] :as state}]
  (assert resource-paths)
  (reduce shadow/step-find-resources
          state
          resource-paths))

(defn initial-build-state
  [config]
  (-> (shadow/init-state)
      (shadow/enable-source-maps)
      (merge config)
      (shadow/step-find-resources-in-jars)
      find-sources
      (shadow/step-finalize-config)
      (shadow/step-compile-core)
      (configure-modules modules)))

(defmulti build-step
  "Build the project, return the new state."
  (fn [type state] type))

(defmethod build-step :dev
  [_ state]
  (try
    (-> state
        (shadow/step-compile-modules)
        (shadow/flush-unoptimized))
    (catch Throwable t
      (prn [:failed-to-compile t])
      (.printStackTrace t)
      state)))

(defmethod build-step :prod
  [_ state]
  (-> state
      (shadow/step-compile-modules)
      (shadow/closure-optimize)
      (shadow/flush-to-disk)
      (shadow/flush-modules-to-disk)))

(defn rebuild-if-changed!
  "Scans the filesystem for changes, rebuilds if necessary, returns the potentially-modified state."
  [build-fn state scan-for-new-files?]
  (let [scan-results (shadow/scan-for-modified-files state scan-for-new-files?)]
    (if (seq scan-results)
      (do (println "Files changed!")
          (build-fn (shadow/reload-modified-files! state scan-results)))
      state)))

(defrecord ShadowBuildWatcher
    [build-key]
  component/Lifecycle
  (start [{:keys [build-key]
           :as component}]
    (assert (get build-config build-key) "Unknown build-type.")
    (println "Starting Shadow Build")
    (let [scanning-channel (chan) ; Closing the scanning-channel causes the scanner to stop.
          build-fn (partial build-step build-key)
          initial-state (->> (build-config build-key)
                             initial-build-state
                             build-fn)]
      (println "Initial build complete. Watching.")
      (go
        (loop [i 0
               state initial-state]
          (let [t (timeout 200)
                [message port] (alts! [scanning-channel t])]
            (when-not (and (= port scanning-channel)
                           (nil? message))
              (recur (mod (inc i) 10)
                     (rebuild-if-changed! build-fn
                                          state
                                          (zero? i)))))))

      (assoc component :scanning-channel scanning-channel)))

  (stop [component]
    (println "Stopping Shadow Build")
    (if-let [channel (:scanning-channel component)]
      (close! channel))
    (assoc component
      :scanning-channel nil
      :build-key nil)))
