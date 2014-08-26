(ns hairball.demo
  (:require [hairball.dom2 :as hb]
            [hairball.vdom :as d :include-macros true]
            [hairball.app :refer [app-get app-swap!]]))

(app-swap! [:todo-items] ["one" "two" "three"])

(defn addTodo []
  (app-swap! [:todo-items] (conj (app-get [:todo-items]) "one more")))


(defn Item [item]
  (d/li item
        " "
        (d/a {:href "#"
              :on-click (fn []
                          (js/console.log "TODO remove" item))}
             "remove")))


(defn App []
  (d/div
   (d/h1 "main app" (app-get [:hello]))
   (d/ul (map Item (app-get [:todo-items])))
   (d/a {:href "#" :on-click addTodo} "add")))

(hb/mount (js/document.getElementById "hairball-mount") App)


(js/setTimeout (fn []
                 (app-swap! [:hello] "world")) 1000)

#_(js/setTimeout (fn []
                 (app-swap! [:todo-items] (range 0 10))) 1000)
