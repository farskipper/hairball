(ns hairball.demo
  (:require [hairball.dom2 :as d]
            [hairball.app :refer [app-get app-swap!]]))


(app-swap! [:todo-items] ["one" "two" "three"])

(defn App []
  [:div {}
   [:h1 {} "main app" [:span {} (app-get [:hello])]]
   (into [] (concat [:ul {}] (map (fn [item]
                           [:li {} (str item)]) (app-get [:todo-items]))))])

(d/mount (js/document.getElementById "hairball-mount") App)


(js/setTimeout (fn []
                 (app-swap! [:hello] "world")) 1000)

(js/setTimeout (fn []
                 (app-swap! [:todo-items] (range 0 10))) 1000)
