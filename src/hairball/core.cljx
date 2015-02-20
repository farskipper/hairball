(ns hairball.core
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [join split replace]]
            [hairball.vdom :refer [Vdom?]]
            #+cljs [hairball.app :refer [app-state app-get app-swap!]]
            #+cljs [goog.dom :as gdom]
            #+cljs [goog.events :as gevnt]
            #+cljs [goog.object :as gobj]))

(defn escape-html [text]
  (-> (str text)
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

(def child-less-tags #{:area
                       :base
                       :br
                       :circle
                       :col
                       :embed
                       :ellipse
                       :hr
                       :img
                       :input
                       :keygen
                       :line
                       :link
                       :meta
                       :param
                       :source
                       :track
                       :wbr})

(def non-escaped-signle-child-only-tags #{:script
                                          :style})

(defn path->id [path]
  (join "." path))

(defn cleanup-attrs
  ([path vdom]         (cleanup-attrs path vdom true))
  ([path vdom add-id?] (cleanup-attrs path vdom add-id? false))
  ([path vdom add-id? add-hash?]
   (let [attrs (apply dissoc (:attrs vdom) [:id :data-hairball-hash])
         attrs (apply hash-map (flatten (filter (fn [[k v]]
                                                  (not (or (= 0 (.indexOf (name k) "on-")) (nil? v)))) attrs)))
         attrs (if add-id?
                 (assoc attrs :id (path->id path))
                 attrs)
         attrs (if add-hash?
                 (assoc attrs :data-hairball-hash (hash vdom))
                 attrs)]
     attrs)))

(defn vdom->string
  ([vdom]              (vdom->string vdom ["root"]))
  ([vdom path]         (vdom->string vdom path true))
  ([vdom path add-id?] (vdom->string vdom path add-id? false))
  ([vdom path add-id? add-hash?]
   (cond
    (Vdom? vdom)
    (do
      (let [tag      (:type vdom)
            attrs    (cleanup-attrs path vdom add-id? add-hash?)
            children (:children vdom)]
        (if (contains? child-less-tags tag)
          (str "<" (name tag) (render-attrs attrs) "/>")
          (str "<" (name tag) (render-attrs attrs) ">"
               (if (or (contains? non-escaped-signle-child-only-tags tag) (true? (get attrs :the-body-of-this-tag-is-raw-html-I-understand-the-risks-when-using-this nil)))
                 (if (string? (first children))
                   (first children)
                   "")
                 (vdom->string children path add-id? add-hash?))
               "</" (name tag) ">"))))

    (or (coll? vdom) (seq? vdom))
    (apply str (map-indexed (fn [i vdom]
                              (vdom->string vdom (concat path [i]) add-id? add-hash?)) vdom))

    (fn? vdom)
    ""

    :else
    (escape-html (str vdom)))))


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
                            old-attrs    (cleanup-attrs path old-vdom)
                            new-attrs    (cleanup-attrs path new-vdom)
                            old-children (into [] (:children old-vdom))
                            new-children (into [] (:children new-vdom))]
                        (if (= old-type new-type)
                          [
                           (if-not (= old-attrs new-attrs)
                             (for [k (set (concat (keys old-attrs) (keys new-attrs)))]
                               (if (and (contains? old-attrs k) (not (contains? new-attrs k)))
                                 (JSop. :remove-attribute path [k])
                                 (if-not (= (get old-attrs k nil) (get new-attrs k))
                                   (JSop. :set-attribute path [k (get new-attrs k)])))))

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
     (filter JSop? (flatten [result])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cljs only
;;
#+cljs
(defn path->element [path]
  (gdom/getElement (path->id path)))

#+cljs
(defn mount-vdom-to-element! [vdom path element]
  (let [tag      (:type vdom)
        attrs    (cleanup-attrs path vdom)
        children (:children vdom)]
    (do
      (gdom/setProperties element (clj->js attrs))
      (if-not (contains? child-less-tags tag)
        (set! (.-innerHTML element) (vdom->string children path))))))

#+cljs
(defn vdom->element [vdom path]
  (let [element (js/document.createElement (name (:type vdom)))]
    (do
      (mount-vdom-to-element! vdom path element)
      element)))

#+cljs
(defn set-attribute! [path attr value]
  (let [element (path->element path)
        value   (if (= :value attr)
                  (if (string? value) value "")
                  value)]
    (if (and (= :value attr) (= value (and element (.-value element))))
      nil;don't re-asign the form input value if it's the same (causes cursors position to change)
      (gdom/setProperties element (js-obj (name attr) value)))))

(def boolean-attrs #{:allowFullScreen
                     :async
                     :autoFocus
                     :autoPlay
                     :checked
                     :controls
                     :defer
                     :disabled
                     :formNoValidate
                     :hidden
                     :loop
                     :multiple
                     :muted
                     :noValidate
                     :readOnly
                     :required
                     :seamless
                     :selected
                     :itemScope})

#+cljs
(defn remove-attribute! [path attr]
  (let [elmt (path->element path)]
    (if (contains? boolean-attrs attr)
      (gdom/setProperties elmt (js-obj (name attr) nil))
      (.removeAttribute elmt (name attr)))))

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

     (= op :set-attribute)
     (set-attribute! path (first args) (second args))

     (= op :remove-attribute)
     (remove-attribute! path (first args))

     (= op :set-content)
     (gdom/setTextContent (path->element path) (first args))

     :else
     (js/console.error (str "unkown op :" (pr-str jsop))))))

#+cljs
(defn apply-JSops-to-dom! [JSops]
  (doseq [jsop JSops]
    (apply-JSop-to-dom! jsop)))


#+cljs
(def last-vdom nil)

#+cljs
(def listenable-events #js [(.-CLICK gevnt/EventType)
                            (.-RIGHTCLICK gevnt/EventType)
                            (.-DBLCLICK gevnt/EventType)
                            (.-MOUSEDOWN gevnt/EventType)
                            (.-MOUSEUP gevnt/EventType)
                            (.-MOUSEOVER gevnt/EventType)
                            (.-MOUSEOUT gevnt/EventType)
                            (.-MOUSEMOVE gevnt/EventType)
                            (.-MOUSEENTER gevnt/EventType)
                            (.-MOUSELEAVE gevnt/EventType)
                            (.-SELECTSTART gevnt/EventType)
                            (.-KEYPRESS gevnt/EventType)
                            (.-KEYDOWN gevnt/EventType)
                            (.-KEYUP gevnt/EventType)
                            (.-BLUR gevnt/EventType)
                            (.-FOCUS gevnt/EventType)
                            (.-DEACTIVATE gevnt/EventType)
                            (.-FOCUSIN gevnt/EventType)
                            (.-FOCUSOUT gevnt/EventType)
                            (.-CHANGE gevnt/EventType)
                            (.-SELECT gevnt/EventType)
                            (.-SUBMIT gevnt/EventType)
                            (.-INPUT gevnt/EventType)
                            (.-PROPERTYCHANGE gevnt/EventType)
                            (.-DRAGSTART gevnt/EventType)
                            (.-DRAG gevnt/EventType)
                            (.-DRAGENTER gevnt/EventType)
                            (.-DRAGOVER gevnt/EventType)
                            (.-DRAGLEAVE gevnt/EventType)
                            (.-DROP gevnt/EventType)
                            (.-DRAGEND gevnt/EventType)
                            (.-TOUCHSTART gevnt/EventType)
                            (.-TOUCHMOVE gevnt/EventType)
                            (.-TOUCHEND gevnt/EventType)
                            (.-TOUCHCANCEL gevnt/EventType)
                            (.-BEFOREUNLOAD gevnt/EventType)
                            (.-CONSOLEMESSAGE gevnt/EventType)
                            (.-CONTEXTMENU gevnt/EventType)
                            (.-DOMCONTENTLOADED gevnt/EventType)
                            (.-ERROR gevnt/EventType)
                            (.-HELP gevnt/EventType)
                            (.-LOAD gevnt/EventType)
                            (.-LOSECAPTURE gevnt/EventType)
                            (.-ORIENTATIONCHANGE gevnt/EventType)
                            (.-READYSTATECHANGE gevnt/EventType)
                            (.-RESIZE gevnt/EventType)
                            (.-SCROLL gevnt/EventType)
                            (.-UNLOAD gevnt/EventType)
                            (.-HASHCHANGE gevnt/EventType)
                            (.-PAGEHIDE gevnt/EventType)
                            (.-PAGESHOW gevnt/EventType)
                            (.-POPSTATE gevnt/EventType)
                            (.-COPY gevnt/EventType)
                            (.-PASTE gevnt/EventType)
                            (.-CUT gevnt/EventType)
                            (.-BEFORECOPY gevnt/EventType)
                            (.-BEFORECUT gevnt/EventType)
                            (.-BEFOREPASTE gevnt/EventType)
                            (.-ONLINE gevnt/EventType)
                            (.-OFFLINE gevnt/EventType)
                            (.-MESSAGE gevnt/EventType)
                            (.-CONNECT gevnt/EventType)
                            (.-ANIMATIONSTART gevnt/EventType)
                            (.-ANIMATIONEND gevnt/EventType)
                            (.-ANIMATIONITERATION gevnt/EventType)
                            (.-TRANSITIONEND gevnt/EventType)
                            (.-POINTERDOWN gevnt/EventType)
                            (.-POINTERUP gevnt/EventType)
                            (.-POINTERCANCEL gevnt/EventType)
                            (.-POINTERMOVE gevnt/EventType)
                            (.-POINTEROVER gevnt/EventType)
                            (.-POINTEROUT gevnt/EventType)
                            (.-POINTERENTER gevnt/EventType)
                            (.-POINTERLEAVE gevnt/EventType)
                            (.-GOTPOINTERCAPTURE gevnt/EventType)
                            (.-LOSTPOINTERCAPTURE gevnt/EventType)
                            (.-MSGESTURECHANGE gevnt/EventType)
                            (.-MSGESTUREEND gevnt/EventType)
                            (.-MSGESTUREHOLD gevnt/EventType)
                            (.-MSGESTURESTART gevnt/EventType)
                            (.-MSGESTURETAP gevnt/EventType)
                            (.-MSGOTPOINTERCAPTURE gevnt/EventType)
                            (.-MSINERTIASTART gevnt/EventType)
                            (.-MSLOSTPOINTERCAPTURE gevnt/EventType)
                            (.-MSPOINTERCANCEL gevnt/EventType)
                            (.-MSPOINTERDOWN gevnt/EventType)
                            (.-MSPOINTERENTER gevnt/EventType)
                            (.-MSPOINTERHOVER gevnt/EventType)
                            (.-MSPOINTERLEAVE gevnt/EventType)
                            (.-MSPOINTERMOVE gevnt/EventType)
                            (.-MSPOINTEROUT gevnt/EventType)
                            (.-MSPOINTEROVER gevnt/EventType)
                            (.-MSPOINTERUP gevnt/EventType)
                            (.-TEXTINPUT gevnt/EventType)
                            (.-COMPOSITIONSTART gevnt/EventType)
                            (.-COMPOSITIONUPDATE gevnt/EventType)
                            (.-COMPOSITIONEND gevnt/EventType)
                            (.-EXIT gevnt/EventType)
                            (.-LOADABORT gevnt/EventType)
                            (.-LOADCOMMIT gevnt/EventType)
                            (.-LOADREDIRECT gevnt/EventType)
                            (.-LOADSTART gevnt/EventType)
                            (.-LOADSTOP gevnt/EventType)
                            (.-RESPONSIVE gevnt/EventType)
                            (.-SIZECHANGED gevnt/EventType)
                            (.-UNRESPONSIVE gevnt/EventType)
                            (.-VISIBILITYCHANGE gevnt/EventType)
                            (.-STORAGE gevnt/EventType)])

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
  (do
    (.stopPropagation e)
    (let [path (split (.-id (.-target e)) #"\.")
          path (rest path)];pop off the root
      (callEventHandlers e last-vdom path))
    false))

#+cljs
(def requestAnimationFrame (if (exists? js/requestAnimationFrame)
                             (fn [cb]
                               (js/requestAnimationFrame cb))
                             (fn [cb]
                               (js/setTimeout cb 16))))

#+cljs
(def ^:private render-queued? false)
#+cljs
(defn mount
  ([render-fn] (mount render-fn js/document.documentElement))
  ([render-fn element]
   ;do the initial render
   (set! last-vdom (render-fn))

   ;mount the initial render to the DOM
   (mount-vdom-to-element! last-vdom ["root"] element)

   ;mount the event system
   (gevnt/listen js/document listenable-events onEvent true)

   ;render when app-state changes
   (let [watch-key   (gensym)
         render!     (fn []
                       (set! render-queued? false)
                       (let [new-vdom (render-fn)]
                         (apply-JSops-to-dom! (vdoms->JSops last-vdom new-vdom))
                         (set! last-vdom new-vdom)))
         queue!render  (fn []
                         (set! render-queued? true)
                         (requestAnimationFrame render!))]
     (add-watch app-state watch-key (fn [_ _ _ _]
                                      (when-not render-queued?
                                        (queue!render)))))))
