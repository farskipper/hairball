(ns hairball.dom
  (:require [hairball.app :refer [app-state app-get app-swap!]]))

(def ^:private main-component-this nil)

(def ^:private refresh-queued false)

(defn render-all []
  (set! refresh-queued false)
  (when (and main-component-this (.isMounted main-component-this))
    (.forceUpdate main-component-this)))

(defn mount [view elm]
  (let [main-component (js/React.createClass #js {:componentWillMount (fn []
                                                                        (this-as this
                                                                                 (set! main-component-this this)))
                                                  :render (fn []
                                                            (view))})
        watch-key (gensym)]
    (add-watch app-state watch-key (fn [_ _ _ _]
                                     (when-not refresh-queued
                                       (set! refresh-queued true)
                                       (if (exists? js/requestAnimationFrame)
                                         (js/requestAnimationFrame render-all)
                                         (js/setTimeout render-all 16)))))
    (. js/React (renderComponent (main-component) elm))))
