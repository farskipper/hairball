(ns hairball.vdom
  (:refer-clojure :exclude [map meta time])
  (:require [clojure.string :refer [join]]
            [hairball.app :refer [app-get app-swap!]])
  #+cljs
  (:require-macros [hairball.vdom :as vdom]))

(defrecord Vdom [type attrs children])
(defn Vdom? [a]
  (= (type a) Vdom))

(defn fix-attrs [attrs]
  (if (empty? attrs)
    nil
    attrs))

(defn attrs? [arg]
  (and (or (map? arg) (nil? arg)) (not (Vdom? arg))))

(defn fix-children [children]
  (let [children (flatten children)
        children (clojure.core/map (fn [child]
                                     (if (or (Vdom? child) (nil? child) (string? child))
                                       child
                                       (str child))) children)
        children (filter (fn [child]
                           (or (Vdom? child) (string? child))) children)]
    (if (= 0 (count (filter Vdom? children)))
      [(join "" (filter string? children))]

      (clojure.core/map (fn [child]
                          (if (string? child)
                            (Vdom. :span nil [child])
                            child)) children))))

#+clj
(def tags '[a
            abbr
            address
            area
            article
            aside
            audio
            b
            base
            bdi
            bdo
            big
            blockquote
            body
            br
            button
            canvas
            caption
            circle
            cite
            code
            col
            colgroup
            data
            datalist
            dd
            defs
            del
            details
            dfn
            div
            dl
            dt
            em
            embed
            fieldset
            figcaption
            figure
            footer
            form
            g
            h1
            h2
            h3
            h4
            h5
            h6
            head
            header
            hr
            html
            i
            iframe
            img
            injection
            input
            ins
            kbd
            keygen
            label
            legend
            li
            line
            linearGradient
            link
            main
            map
            mark
            menu
            menuitem
            meta
            meter
            nav
            noscript
            object
            ol
            optgroup
            option
            output
            p
            param
            path
            polygon
            polyline
            pre
            progress
            q
            radialGradient
            rect
            rp
            rt
            ruby
            s
            samp
            script
            section
            select
            small
            source
            span
            stop
            strong
            style
            sub
            summary
            sup
            svg
            table
            tbody
            td
            text
            textarea
            tfoot
            th
            thead
            time
            title
            tr
            track
            u
            ul
            var
            video
            wbr])

#+clj
(defmacro gen-dom-fns []
  `(do
     ~@(clojure.core/map
        (fn [tag]
          `(defn ~tag [& args#]
             (let [attrs#    (if (attrs? (first args#))
                               (first args#))
                   children# (if (attrs? (first args#))
                               (rest args#)
                               args#)]
               (Vdom. ~(keyword (name tag)) (fix-attrs attrs#) (fix-children children#)))))
        tags)))

#+clj
(gen-dom-fns)

#+cljs
(vdom/gen-dom-fns)

(defn Input [data-path & [type attrs]]
  (let [type       (or type "text")
        attrs      (if (map? attrs)
                     attrs
                     {})
        bindInput! (fn [e]
                     (app-swap! data-path (.-value (.-target e))))]
    ;TODO suport type "gdate" (use google's date picker)
    (cond
     (= "select" type)
     (let [options     (:options attrs);a list of key value pairs
           options     (cons [nil (get attrs :placeholder "")] options);add the placeholder as the first option
           attrs       (dissoc attrs :options)
           value       (app-get data-path)
           value       (if (contains? (into #{} (clojure.core/map first options)) value)
                         value
                         nil)
           bindInput!  (fn [e]
                         (app-swap! data-path (first (nth options (int (.-value (.-target e))) [nil]))))]
       (div
        (pr-str (app-get data-path)) " = " (pr-str value) " " (div (into #{} (clojure.core/map first options)))

       (select (merge {:on-no-prevent-change bindInput!} attrs)
               (map-indexed (fn [i [v text]]
                              (option (if (= v value)
                                        {:value i :selected "selected"}
                                        {:value i}) text)) options))))

     (= "textarea" type)
     (textarea (merge
                {:on-no-prevent-change bindInput!
                 :on-no-prevent-keyup  bindInput!
                 :on-no-prevent-input  bindInput!
                 :on-no-prevent-cut    bindInput!
                 :on-no-prevent-paste  bindInput!
                 :value                (app-get data-path)}
                attrs) (app-get data-path))

     :else
     (input (merge
             {:type type
              :on-no-prevent-change bindInput!
              :on-no-prevent-keyup  bindInput!
              :on-no-prevent-input  bindInput!
              :on-no-prevent-cut    bindInput!
              :on-no-prevent-paste  bindInput!
              :value                (app-get data-path)}
             attrs)))))
