(ns hairball.dom
  (:refer-clojure :exclude [map meta time])
  (:require [clojure.string :as str]
            cljs.core)
  (:import [cljs.tagged_literals JSValue]))


(defn clj->js [v]
  (JSValue. v))

(defn jsObject? [x]
  (instance? JSValue x))

(def orig-map clojure.core/map)

(defn kebab->camel [s]
  (str/replace s #"-(\w)" (comp str/upper-case second)))

(defn prop-key-case [attr]
  (if (re-find #"^(data\-|aria\-)" attr)
    attr
    (kebab->camel attr)))

(defn prop-key-alias [prop]
  (case prop
    :class :className
    :for :htmlFor
    prop))

(defn fix-prop-key [prop-key]
  (-> prop-key
      name
      prop-key-alias
      prop-key-case
      keyword))

(defn fix-prop-val [prop-val]
  (if (map? prop-val)
    (clj->js prop-val)
    prop-val))

(defn fix-props [props]
  (if (jsObject? props)
    props
    (clj->js (into {} (orig-map (fn [[k v]]
                                  [(fix-prop-key k) (fix-prop-val v)]) props)))))

(defn props? [arg]
  (or (map? arg) (nil? arg) (jsObject? arg)))


;To generate this list of tags, just run this:
;  Object.keys(React.DOM).sort().join("\n            ")
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

(defmacro gen-dom-macros []
  `(do
     ~@(orig-map (fn [tag]
                   `(defmacro ~(symbol "hairball.dom" tag) [& args#]
                      (let [react-call# '~(symbol "js" (str "React.DOM." tag))
                            props# (if (props? (first args#))
                                     (fix-props (first args#))
                                     nil)
                            children# (if (props? (first args#))
                                        (rest args#)
                                        args#)]
                        ;TODO optimize this so we don't make a vector and flatten it on every single render i.e. (dom/div "hello" (dom/div "world")) shouldn't
                        `(apply ~react-call# ~props# (flatten (vector ~@children#)))))) (orig-map name tags))))
(gen-dom-macros)
