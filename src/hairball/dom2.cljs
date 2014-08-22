(ns hairball.dom2
  (:require [clojure.string :refer [join]]
            [goog.dom :as gdom]))

(defrecord JSop [op path args])
(defn JSop? [a]
  (= (type a) JSop))

(defn path->element [path]
  (gdom/getElement (join "." path)))

(defn apply-JSop-to-dom! [jsop]
  (let [op   (:op jsop)
        path (:path jsop)
        args (:args jsop)]
    (cond
     (= op :insert-child)
     (gdom/insertChildAt (path->element path) (gdom/htmlToDocumentFragment "<h1>fake</h1>") 0)

     :else
     (js/console.log (str "unkown op :" (name op))))))


;call google closure to make the following changes..
;    [:insert-child];goog.dom.insertChildAt(parent, child, index)
;    [:replace-node];goog.dom.replaceNode(newNode, oldNode)
;    [:remove-node];goog.dom.removeNode(node)
;
;    [:set-properties];goog.dom.setProperties(element, properties)
;    [:set-content];goog.dom.setTextContent(node, text)
;    [:set-content-dangerously];goog.dom.htmlToDocumentFragment(htmlString)

(defn apply-JSops-to-dom! [JSops]
  (doseq [jsop JSops]
    (apply-JSop-to-dom! jsop)))

(defn mount [element render-fn]
  (do
    (gdom/setProperties element #js {:id "root"})
    (apply-JSops-to-dom! [(JSop. :insert-child ["root"] [(render-fn)])])))
