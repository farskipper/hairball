(ns hairball.demo
  (:require [hairball.dom2 :as hb]
            [hairball.vdom :as d :include-macros true]
            [hairball.app :refer [app-get app-swap!]]))


(js/console.log (pr-str
                 (d/div
                  (d/h1 "main app" (d/span (app-get [:hello])))

                  (apply d/li (map (fn [item]
                                     (d/li (str item))) (app-get [:todo-items]))))))


(app-swap! [:todo-items] ["one" "two" "three"])

(defn App []
  [:div {}
   [:h1 {} "main app" [:span {} (app-get [:hello])]]
   (into [] (concat [:ul {}] (map (fn [item]
                           [:li {} (str item)]) (app-get [:todo-items]))))])

(hb/mount (js/document.getElementById "hairball-mount") App)


(js/setTimeout (fn []
                 (app-swap! [:hello] "world")) 1000)

(js/setTimeout (fn []
                 (app-swap! [:todo-items] (range 0 10))) 1000)
