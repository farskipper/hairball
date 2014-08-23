(ns hairball.demo
  (:require [hairball.dom2 :as hb]
            [hairball.vdom :as d :include-macros true]
            [hairball.app :refer [app-get app-swap!]]))

(app-swap! [:todo-items] ["one" "two" "three"])

(js/console.log (pr-str (apply d/div ["one" "two"])))

(js/console.log (pr-str (apply d/li (into [] (map (fn [item]
                                                    (d/li (str item))) (app-get [:todo-items]))))))
(js/console.log (pr-str
                 (d/div
                  (d/h1 "main app" (d/span (app-get [:hello])))
                  (d/h2 "one" "two" "three")

                  (apply d/li (map (fn [item]
                                     (d/li (str item))) (app-get [:todo-items]))))))


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
