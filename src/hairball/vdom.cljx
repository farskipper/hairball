(ns hairball.vdom
  (:refer-clojure :exclude [map meta time])
  (:require [hairball.dom2 :refer [Vdom?]])
  #+cljs
  (:require-macros [hairball.vdom :as vdom]))

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

(defn fix-props [props]
  ;TODO flesh this out more (i.e. event handling)
  props)

(defn props? [arg]
  (and (or (map? arg) (nil? arg)) (not (Vdom? arg))))

#+clj
(defmacro gen-dom-fns []
  `(do
     ~@(clojure.core/map
        (fn [tag]
          `(defn ~(symbol (name tag)) [& args#]
             (let [attrs#    (if (props? (first args#))
                               (fix-props (first args#)))
                   children# (if (props? (first args#))
                               (rest args#)
                               args#)]
               (hairball.dom2.Vdom. ~tag attrs# (flatten (vector children#))))))
        tags)))

#+clj
(gen-dom-fns)

#+cljs
(vdom/gen-dom-fns)
