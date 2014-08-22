(ns hairball.dom2
  (:require [clojure.string :refer [join]]))

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

;NOTE
;NOTE vdom will not be made by hand, so don't be too worried about checking it
;NOTE
(defn vdom->string
  ([vdom] (vdom->string vdom false ["root"]))
  ([vdom plain] (vdom->string vdom plain ["root"]))
  ([vdom plain path]
   (if-not (vector? vdom)
     (escape-html (str vdom))
     (let [tag      (first vdom)
           attrs    (second vdom)
           attrs    (if plain
                      attrs
                      (merge {:data-hairball-id   (join "." path)
                              :data-hairball-hash (hash vdom)} attrs))
           children (rest (rest vdom))]

       (if (contains? child-less-tags tag)
         (str "<" (name tag) (render-attrs attrs) "/>")
         (str "<" (name tag) (render-attrs attrs) ">"
              (apply str (map-indexed (fn [i vdom]
                                        (vdom->string vdom plain (conj path i))) children))
              "</" (name tag) ">"))))))

(defn all-childrens-keys [children1 children2]
  (range (max (count children1) (count children2))))



(defrecord JSop [op path args])
(defn JSop? [a]
  (= (type a) JSop))

(defn vdoms->JSops
  ([old-vdom new-vdom] (vdoms->JSops old-vdom new-vdom ["root"]))
  ([old-vdom new-vdom path]
   #_(println (pr-str [old-vdom new-vdom path]))
   (let [parent_path (butlast path)
         result      (cond
                      ;no diff
                      (= old-vdom new-vdom) nil

                      ;two vdoms
                      (and (vector? old-vdom) (vector? new-vdom)) (let [old-type     (first old-vdom)
                                                                        new-type     (first new-vdom)
                                                                        old-attrs    (second old-vdom)
                                                                        new-attrs    (second new-vdom)
                                                                        old-children (into [] (rest (rest old-vdom)))
                                                                        new-children (into [] (rest (rest new-vdom)))]
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
                      (and (vector? old-vdom) (string? new-vdom)) [(JSop. :remove-node  path [])
                                                                   (JSop. :set-content parent_path [new-vdom])]
                      ;string to vdom
                      (and (string? old-vdom) (vector? new-vdom)) [(JSop. :set-content parent_path [""])
                                                                   (JSop. :insert-child  parent_path [new-vdom 0])]


                      ;changed content
                      (and (string? old-vdom) (string? new-vdom)) (JSop. :set-content parent_path [new-vdom])
                      (and (nil? old-vdom) (string? new-vdom)) (JSop. :set-content parent_path [new-vdom])
                      (and (string? old-vdom) (nil? new-vdom)) (JSop. :set-content parent_path [new-vdom])


                      ;removed vdom
                      (and (vector? old-vdom) (nil? new-vdom)) (JSop. :remove-node path [])

                      ;added vdom
                      (and (nil? old-vdom) (vector? new-vdom)) (JSop. :insert-child parent_path [new-vdom (last path)])


                      :else nil)]
     (if (or (vector? result) (list? result));can't use "coll?" b/c JSop is a coll
       (filter JSop? (flatten result))
       [result]))))
