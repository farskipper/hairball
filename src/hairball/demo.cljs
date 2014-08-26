(ns hairball.demo
  (:require [hairball.dom2 :as hb]
            [hairball.vdom :as d :include-macros true]
            [hairball.app :refer [app-get app-swap!]]))

(app-swap! [:todo-items] ["one" "two" "three"])
(app-swap! [:new-item] "new item")

(defn addTodo []
  (app-swap! [:todo-items] (conj (app-get [:todo-items]) (app-get [:new-item])))
  (app-swap! [:new-item] ""))


(defn Item [item]
  (d/li item
        " "
        (d/a {:href "#"
              :on-click (fn []
                          (js/console.log "TODO remove" item))}
             "remove")))


(defn Input [type data-path]
  (let [bindInput (fn [e]
                    (app-swap! data-path (.-value (.-target e))))]
    (d/input {:type type
              :on-no-prevent-change bindInput
              :on-no-prevent-keyup  bindInput
              :on-no-prevent-input  bindInput
              :on-no-prevent-cut    bindInput
              :on-no-prevent-paste  bindInput
              :value (app-get data-path)})))

(defn App []
  (d/div
   (d/h1 "main app" (app-get [:hello]))
   (d/ul (map Item (app-get [:todo-items])))
   (Input "text" [:new-item])
   (d/div (app-get [:new-item]))
   (Input "text" [:new-item])
   (d/a {:href "#" :on-click addTodo} "add")))

(hb/mount (js/document.getElementById "hairball-mount") App)


(js/setTimeout (fn []
                 (app-swap! [:hello] "world")) 1000)

#_(js/setTimeout (fn []
                 (app-swap! [:todo-items] (range 0 10))) 1000)
