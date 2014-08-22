(ns hairball.demo
  (:require [hairball.dom2 :as d]))

(defn App []
  [:h1 {} "main app"])

(d/mount js/document.body App)
