(ns hairball.demo
  (:require [hairball.core :as hb]
            [hairball.vdom :as d :include-macros true]
            [hairball.app :refer [app-get app-swap!]]))

(app-swap! [:todo-items] ["one" "two" "three"])
(app-swap! [:new-item] "new item")

(defn addTodo []
  (app-swap! [:todo-items] (conj (app-get [:todo-items]) (app-get [:new-item])))
  (app-swap! [:new-item] ""))


(defn Item [item]
  (d/li (d/span item)
        " - "
        (d/a {:href "#"
              :on-click (fn []
                          (js/console.log "TODO remove" item))}
             "remove")))


(defn App []
  (d/div {:class "container"
          :on-click (fn []
                      (js/console.log "clicked the continer"))}
   (d/h1 "main app" (app-get [:hello]))
   (d/ul (map Item (app-get [:todo-items])))
   (d/Input [:new-item] "text" {:class "form-control"})
   (d/pre (app-get [:new-item]))
   (d/Input [:new-item] "textarea" {:rows 8 :class "form-control"})
   (d/a {:href "#" :on-click addTodo :class "btn btn-default"} "add")
   (d/form {:on-submit (fn []
                         (js/console.log "submit"))}
           (d/Input [:old-item] "text"))))

(hb/mount (js/document.getElementById "hairball-mount") App)


(js/setTimeout (fn []
                 (app-swap! [:hello] "world")) 1000)

(js/setTimeout (fn []
                 (app-swap! [:todo-items] (range 0 3))) 1000)
