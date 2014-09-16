(ns hairball.demo
  (:require [hairball.core :as hb]
            [hairball.vdom :as d :include-macros true]
            [hairball.app :refer [app-get app-swap!]]))

(app-swap! [:todo-items] ["one" "two" "three"])
(app-swap! [:new-item] "new \"item\"")

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
   (d/h1 {:class (app-get [:title-class])} "main app" (app-get [:hello]))
   (d/ul (map Item (app-get [:todo-items])))
   (d/Input [:new-item] "text" {:class "form-control"})
   (d/pre (app-get [:new-item]))
   (d/Input [:new-item] "textarea" {:rows 8 :class "form-control"})
   (d/a {:href "#" :on-click addTodo :class "btn btn-default"} "add")
   (d/form {:on-submit (fn []
                         (js/console.log "submit"))}
           (d/div {:class "row"}
                  (d/div {:class "col-sm-6"}
                         (d/Input [:demo-select] "select" {:class   "form-control"
                                                           :options [["key"  "value"]
                                                                     ["asdf" "Asdf"]]}))
                  (d/div {:class "col-sm-6"}
                         (d/Input [:demo-select] "text" {:class "form-control"}))))
   (d/table {:class "table"}
            (d/thead
             (d/tr
              (d/th "Title 1")
              (d/th "Title 2")
              (d/th "Title 3")))
            (d/tbody
             (if (empty? (app-get [:table-rows]))
               (d/tr
                (d/td {:colspan 3}
                      "no rows to show"))
               (map (fn [row]
                      (d/tr
                       (d/td {:colspan nil} "row#" row)
                       (d/td "some")
                       (d/td "data"))) (app-get [:table-rows])))))))

(hb/mount (js/document.getElementById "hairball-mount") App)


(js/setTimeout (fn []
                 (app-swap! [:hello] "world")) 1000)

(js/setTimeout (fn []
                 (app-swap! [:todo-items] (range 0 3))) 1000)

(js/setTimeout (fn []
                 (app-swap! [:table-rows] (range 0 5))) 2000)

(js/setTimeout (fn []
                 (app-swap! [:title-class] "text-muted")) 3000)

(js/setTimeout (fn []
                 (app-swap! [:title-class] nil)) 6000)
