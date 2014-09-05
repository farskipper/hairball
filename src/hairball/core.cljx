(ns hairball.core
  (:require [clojure.string :refer [join split]]
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
    (replace "'"  "&apos;")))

(defn render-attr [[key val]]
  (str " " (name key) "=\"" (escape-html val) "\""))

(defn render-attrs [attrs]
  (if-not (map? attrs)
    ""
    (apply str (map render-attr attrs))))

(def child-less-tags #{:br :input :img :circle :rect :line :ellipse})

(defn sanitize-attrs [attrs]
  (apply hash-map
         (flatten
          (filter
           (fn [[k v]]
             (not= 0 (.indexOf (name k) "on-")))
           (apply dissoc attrs [:id :data-hairball-hash])))))

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
                                          (vdom->string vdom plain (concat path [i]))) children))
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
                                             (concat path [k]))))
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
    (cond
     (= op :insert-child)
     (gdom/insertChildAt (path->element path) (vdom->element (first args) (concat path [(second args)])) (second args))

     (= op :replace-node)
     (gdom/replaceNode (vdom->element (first args) path) (path->element path))

     (= op :remove-node)
     (gdom/removeNode (path->element path))

     (= op :set-properties)
     (gdom/setProperties (path->element path) (clj->js (first args)))

     (= op :set-content)
     (gdom/setTextContent (path->element path) (first args))

     :else
     (js/console.error (str "unkown op :" (name op))))))

#+cljs
(defn apply-JSops-to-dom! [JSops]
  (doseq [jsop JSops]
    (apply-JSop-to-dom! jsop)))


#+cljs
(def last-vdom nil)

#+cljs
(def listenable-events #js [(.-CLICK  gevnt/EventType)

                            (.-CHANGE gevnt/EventType)
                            (.-KEYUP gevnt/EventType)
                            (.-INPUT gevnt/EventType)
                            (.-CUT gevnt/EventType)
                            (.-PASTE gevnt/EventType)

                            (.-RESIZE gevnt/EventType)
                            ]);(gobj/getValues gevnt/EventType))

#+cljs
(defn callEventHandlers [e vdom path]
  ;first recurse down the dom tree to the given path
  (let [curr-index (first path)]
    (if-not (nil? curr-index)
      (callEventHandlers e (nth (:children vdom) (js/parseInt curr-index) nil) (rest path))))

  ;then as the recursion pops off the call stack "bubble up" the event handlers
  (let [attrs    (:attrs vdom)
        handler  (get attrs (keyword (str "on-" (.-type e))))
        nhandler (get attrs (keyword (str "on-no-prevent-" (.-type e))))]
    (if (fn? nhandler)
      (nhandler e)

      (if (fn? handler)
        (do
          (.preventDefault e)
          (handler e)
          false)))))

#+cljs
(defn onEvent [e]
  (let [path (split (.-id (.-target e)) #"\.")
        path (rest path)];pop off the root
    (do
      (.stopPropagation e)
      (callEventHandlers e last-vdom path)
      false)))

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

