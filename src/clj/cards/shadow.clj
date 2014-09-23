(ns cards.shadow
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            ;; [weasel.repl.websocket :as weasel]
            [shadow.cljs.build :as shadow]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Shadowbuild.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def modules
  [[:cljs '[cljs.core clojure.set clojure.string cljs.core.async] #{}]
   [:dev '[
           speclj.core
           weasel.repl
           ]
    #{:cljs}]
   [:cards '[cards.core
             sablono.core
             no.en.core inflections.core
             ] #{:cljs}]])

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

;; (defn dev-build
;;   []
;;   (loop [state (initial-dev-state)]
;;     (let [new-state (-> state
;;                         dev-build-step
;;                         shadow/wait-and-reload!)]
;;       (recur new-state))))

(defrecord ShadowBuild
    []

  component/Lifecycle
  (start [component]
    (println "Starting Shadow Build")
    (assoc component :state (dev-build-step (initial-dev-state))))

  (stop [component]
    (println "Stopping Shadow Build")
    (dissoc component :state)))
