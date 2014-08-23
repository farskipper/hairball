(ns hairball.dom2
  (:require [clojure.string :refer [join]]
            [hairball.vdom :refer [Vdom?]]
            #+cljs [hairball.app :refer [app-state app-get app-swap!]]
            #+cljs [goog.dom :as gdom]
            #+cljs [goog.events :as gevnt]
            #+cljs [goog.object :as gobj]))

(defn escape-html [text]
  (.. (str text)
    (replace "&"  "&amp;")
    (replace "<"  "&lt;")
    (replace ">"  "&gt;")
    (replace "\"" "&quot;")
    (replace "'"  "&apos;")));"&#39;"-lodash or "&#x27;"-react

(defn render-attr [[key val]]
  (str " " (name key) "=\"" (escape-html val) "\""))

(defn render-attrs [attrs]
  (if-not (map? attrs)
    ""
    (apply str (map render-attr attrs))))

(def child-less-tags #{:br :input :img :circle :rect :line :ellipse})


#+cljs
(def event-listeners (atom {}))
#+cljs
(def listenable-events #js [(.-CLICK  gevnt/EventType)
                            (.-CHANGE gevnt/EventType)
                            (.-RESIZE gevnt/EventType)
                            ]);(gobj/getValues gevnt/EventType))
#+cljs
(defn attachListeners! [attrs path]
  (doseq [event-name listenable-events]
    (let [kw (keyword (str "on-" event-name))]
      (if (contains? attrs kw)
        (swap! event-listeners assoc-in [(join "." path) event-name] (kw attrs))))))

(defn sanitize-attrs [attrs]
  (apply dissoc attrs [:id :data-hairball-hash :on-click]))


;NOTE
;NOTE vdom will not be made by hand, so don't be too worried about checking it
;NOTE
(defn vdom->string
  ([vdom] (vdom->string vdom false ["root"]))
  ([vdom plain] (vdom->string vdom plain ["root"]))
  ([vdom plain path]
   (if-not (Vdom? vdom)
     (escape-html (str vdom))
     (do
       #+cljs (attachListeners! (:attrs vdom) path)

       (let [tag      (:type vdom)
             attrs    (:attrs vdom)
             attrs    (sanitize-attrs attrs)
             attrs    (if plain
                        attrs
                        (merge {:id                 (join "." path)
                                :data-hairball-hash (hash vdom)} attrs))
             children (:children vdom)]
         (if (contains? child-less-tags tag)
           (str "<" (name tag) (render-attrs attrs) "/>")
           (str "<" (name tag) (render-attrs attrs) ">"
                (apply str (map-indexed (fn [i vdom]
                                          (vdom->string vdom plain (conj path i))) children))
                "</" (name tag) ">")))))))

(defn all-childrens-keys [children1 children2]
  (range (max (count children1) (count children2))))



(defrecord JSop [op path args])
(defn JSop? [a]
  (= (type a) JSop))

(defn vdoms->JSops
  ([old-vdom new-vdom] (vdoms->JSops old-vdom new-vdom ["root"]))
  ([old-vdom new-vdom path]
   (let [parent_path (butlast path)
         result      (cond
                      ;no diff
                      (= old-vdom new-vdom)
                      nil

                      ;two vdoms
                      (and (Vdom? old-vdom) (Vdom? new-vdom))
                      (let [old-type     (:type old-vdom)
                            new-type     (:type new-vdom)
                            old-attrs    (sanitize-attrs (:attrs old-vdom))
                            new-attrs    (sanitize-attrs (:attrs new-vdom))
                            old-children (into [] (:children old-vdom))
                            new-children (into [] (:children new-vdom))]
                        (if (= old-type new-type)
                          [
                           (if-not (= old-attrs new-attrs)
                             (JSop. :set-properties path [new-attrs]))

                           (if-not (= old-children new-children)
                             (for [k (all-childrens-keys old-children new-children)]
                               (vdoms->JSops (get old-children k)
                                             (get new-children k)
                                             (conj path k))))
                           ]

                          ;if it's a new type of node, just replace it
                          (JSop. :replace-node path [new-vdom])))

                      ;vdom to string
                      (and (Vdom? old-vdom) (string? new-vdom))
                      [(JSop. :remove-node  path [])
                       (JSop. :set-content parent_path [new-vdom])]

                      ;string to vdom
                      (and (string? old-vdom) (Vdom? new-vdom))
                      [(JSop. :set-content parent_path [""])
                       (JSop. :insert-child  parent_path [new-vdom 0])]


                      ;changed content
                      (and (string? old-vdom) (string? new-vdom))
                      (JSop. :set-content parent_path [new-vdom])

                      (and (nil? old-vdom) (string? new-vdom))
                      (JSop. :set-content parent_path [new-vdom])

                      (and (string? old-vdom) (nil? new-vdom))
                      (JSop. :set-content parent_path [new-vdom])


                      ;removed vdom
                      (and (Vdom? old-vdom) (nil? new-vdom))
                      (JSop. :remove-node path [])

                      ;added vdom
                      (and (nil? old-vdom) (Vdom? new-vdom))
                      (JSop. :insert-child parent_path [new-vdom (last path)])


                      :else nil)]
     (if (or (vector? result) (list? result));can't use "coll?" b/c JSop is a coll
       (filter JSop? (flatten result))
       [result]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cljs only
;;
#+cljs
(defn path->element [path]
  (gdom/getElement (join "." path)))

#+cljs
(defn vdom->element [vdom path]
  ;TODO make this so it can make <body> <html> <head> tags so you can mount the whole page
  (gdom/htmlToDocumentFragment (vdom->string vdom false path)))

#+cljs
(defn apply-JSop-to-dom! [jsop]
  (let [op   (:op jsop)
        path (:path jsop)
        args (:args jsop)]
    (js/console.log (name op))
    (cond
     (= op :insert-child)
     (gdom/insertChildAt (path->element path) (vdom->element (first args) (concat path [(second args)])) (second args))

     (= op :replace-node)
     (gdom/replaceNode (vdom->element (first args) path) (path->element path))

     (= op :remove-node)
     (gdom/removeNode (path->element path))

     (= op :set-properties)
     (gdom/setTextContent (path->element path) (clj->js (first args)))

     (= op :set-content)
     (gdom/setTextContent (path->element path) (first args))

     (= op :set-properties)
     (gdom/setTextContent (path->element path) (first args))

     :else
     (js/console.log (str "unkown op :" (name op))))))

#+cljs
(defn apply-JSops-to-dom! [JSops]
  (doseq [jsop JSops]
    (apply-JSop-to-dom! jsop)))


#+cljs
(defn onEvent [e]
  (let [path    (.-id (.-target e))
        type    (.-type e)
        handler (get-in @event-listeners [path type])]
    (if (fn? handler)
      (do
        (.stopPropagation e)
        (.preventDefault e)
        (handler e)
        false))))

#+cljs
(def last-vdom nil)
#+cljs
(def ^:private refresh-queued? false)
#+cljs
(defn mount [element render-fn]
  (gdom/setProperties element #js {:id "root"})
  (set! last-vdom (render-fn))
  (apply-JSops-to-dom! [(JSop. :replace-node ["root"] [last-vdom])])
  (gevnt/listen (path->element ["root"]) listenable-events onEvent true)
  (let [watch-key (gensym)
        render!   (fn []
                    (set! refresh-queued? false)
                    (let [new-vdom (render-fn)]
                      (apply-JSops-to-dom! (vdoms->JSops last-vdom new-vdom))
                      (set! last-vdom new-vdom)))]
    (add-watch app-state watch-key (fn [_ _ _ _]
                                     (when-not refresh-queued?
                                       (set! refresh-queued? true)
                                       (if (exists? js/requestAnimationFrame)
                                         (js/requestAnimationFrame render!)
                                         (js/setTimeout render! 16)))))))

