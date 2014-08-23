(ns hairball.vdom
  (:refer-clojure :exclude [map meta time])
  (:require [clojure.string :refer [join]])
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
  (let [children (clojure.core/map (fn [child]
                                     (if (or (Vdom? child) (nil? child) (string? child))
                                       child
                                       (str child)))
                                   (flatten children))
        vdoms    (filter Vdom? children)
        text     (join "" (filter string? children))]
    (if (> (count text) 0)
      (conj vdoms text)
      vdoms)))

#+clj
(def tags '[:a
            :abbr
            :address
            :area
            :article
            :aside
            :audio
            :b
            :base
            :bdi
            :bdo
            :big
            :blockquote
            :body
            :br
            :button
            :canvas
            :caption
            :circle
            :cite
            :code
            :col
            :colgroup
            :data
            :datalist
            :dd
            :defs
            :del
            :details
            :dfn
            :div
            :dl
            :dt
            :em
            :embed
            :fieldset
            :figcaption
            :figure
            :footer
            :form
            :g
            :h1
            :h2
            :h3
            :h4
            :h5
            :h6
            :head
            :header
            :hr
            :html
            :i
            :iframe
            :img
            :injection
            :input
            :ins
            :kbd
            :keygen
            :label
            :legend
            :li
            :line
            :linearGradient
            :link
            :main
            :map
            :mark
            :menu
            :menuitem
            :meta
            :meter
            :nav
            :noscript
            :object
            :ol
            :optgroup
            :option
            :output
            :p
            :param
            :path
            :polygon
            :polyline
            :pre
            :progress
            :q
            :radialGradient
            :rect
            :rp
            :rt
            :ruby
            :s
            :samp
            :script
            :section
            :select
            :small
            :source
            :span
            :stop
            :strong
            :style
            :sub
            :summary
            :sup
            :svg
            :table
            :tbody
            :td
            :text
            :textarea
            :tfoot
            :th
            :thead
            :time
            :title
            :tr
            :track
            :u
            :ul
            :var
            :video
            :wbr])

#+clj
(defmacro gen-dom-fns []
  `(do
     ~@(clojure.core/map
        (fn [tag]
          `(defn ~(symbol (name tag)) [& args#]
             (let [attrs#    (if (attrs? (first args#))
                               (first args#))
                   children# (if (attrs? (first args#))
                               (rest args#)
                               args#)]
               (Vdom. ~tag (fix-attrs attrs#) (fix-children children#)))))
        tags)))

#+clj
(gen-dom-fns)

#+cljs
(vdom/gen-dom-fns)
