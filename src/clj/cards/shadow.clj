(ns cards.shadow
  (:require [clojure.core.async :refer [go go-loop timeout alts! <! chan close!]]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [shadow.cljs.build :as shadow]
            [clojure.java.shell :refer [sh]]
            [clojurescript.test.plugin :as cljs-test]))


;;; TODO These config items should be separate.
(def modules
  [[:cljs '[cljs.core
            clojure.set clojure.string cljs.core.async cljs.reader
            inflections.core
            datascript
            sablono.core
            om.core] #{}]
   [:cards '[cards.core] #{:cljs}]
   [:devel '[cards.devel] #{:cards}]
   [:test '[cards.ui-messages-test] #{:cards}]])

(def build-config
  {:dev {:resource-paths ["src/cljs" "test/cljs"]
         :optimizations :simple
         :pretty-print false
         :work-dir (io/file "target/cljs-work")
         :public-dir (io/file "resources/out")}
   :prod {:resource-paths ["src/cljs" "test/cljs"]
          :optimizations :advanced
          :pretty-print false
          :work-dir (io/file "target/cljs-work")
          :public-dir (io/file "resources/out")
          :preamble ["react/react.js"]
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
      (shadow/flush-modules-to-disk)))

(defn rebuild-if-changed!
  "Scans the filesystem for changes, rebuilds if necessary, returns the potentially-modified state."
  [build-fn state scan-for-new-files?]
  (let [scan-results (concat (shadow/scan-for-modified-files state)
                             (when scan-for-new-files?
                               (shadow/scan-for-new-files state)))]
    (if (seq scan-results)
      (do (println "Files changed!")
          (build-fn (shadow/reload-modified-files! state scan-results)))
      state)))

(defrecord ShadowBuildWatcher
    [build-type]
  component/Lifecycle
  (start [{:keys [build-type]
           :as component}]
    (assert (get build-config build-type) (format "Unknown build-type: '%s'" build-type))
    (println "Starting Shadow Build")
    (let [scanning-channel (chan) ; Closing the scanning-channel causes the scanner to stop.
          build-fn (partial build-step build-type)
          initial-state (->> (build-config build-type)
                             initial-build-state
                             build-fn)]
      (println "Initial build complete. Watching...")
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
      :build-type nil)))
