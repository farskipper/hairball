(ns hairball.demo
  (:require [hairball.core :as hb]
            [hairball.vdom :as d :include-macros true]
            [hairball.app :refer [app-get app-swap!]]))

(app-swap! [:todo-items] [{:text "one"} {:done true  :text "two"} {:text "three"}])
(app-swap! [:new-item] "new \"item\"")

(defn addTodo []
  (app-swap! [:todo-items] (conj (app-get [:todo-items])
                                 {:done false
                                  :text (app-get [:new-item])}))
  (app-swap! [:new-item] ""))


(defn drop-nth [n coll]
  (let [v (if (vector? coll)
            coll
            (vec coll))]
    (vec (concat (subvec v 0 n) (subvec v (inc n) (count v))))))

(defn Item [i _]
  (d/li (d/Input [:todo-items i :done] "checkbox")
        (d/Input [:todo-items i :text] "text")
        (d/a {:href "#"
              :on-click (fn []
                          (app-swap! [:todo-items] (drop-nth i (app-get [:todo-items]))))}
             "remove")))


(defn App []
  (d/div {:class "container"
          :on-click (fn []
                      (js/console.log "clicked the continer"))}
         (d/pre (pr-str (app-get [])))
         (d/h1 {:class (app-get [:title-class])} "main app" (app-get [:hello]))
         (d/ul (map-indexed Item (app-get [:todo-items])))
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
         (d/div {:the-body-of-this-tag-is-raw-html-I-understand-the-risks-when-using-this true}
                "<h1>raw-html</h1>")
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

(hb/mount App (js/document.getElementById "hairball-mount"))
#_
(hb/mount (fn []
            (d/html {:lang "en"}
                    (d/head
                     (d/title "some title")
                     (d/link {:rel "stylesheet" :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css"})
                     (d/link {:rel "stylesheet" :href "https://maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css"}))
                    (d/body
                     (App)))))


(js/setTimeout (fn []
                 (app-swap! [:hello] "world")) 1000)

(js/setTimeout (fn []
                 (app-swap! [:table-rows] (range 0 5))) 2000)

(js/setTimeout (fn []
                 (app-swap! [:title-class] "text-muted")) 3000)

(js/setTimeout (fn []
                 (app-swap! [:title-class] nil)) 6000)
