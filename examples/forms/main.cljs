(ns hairball.example.main
  (:require [hairball.core :as hb]
            [hairball.vdom :as d :include-macros true]
            [hairball.app :refer [app-get app-swap!]]))

(defn App []
  (d/div {:style "margin:20px auto;width:300px;"}
         (d/div
           "All these inputs are bound to the same state"
           (d/pre (pr-str (app-get [:input])))
           (d/a {:href "#" :on-click (fn []
                                       (app-swap! [:input] ""))} "clear"))
         "Text input with an enter key event"
         (d/Input [:input] "text" {:class "form-control"
                                   :on-no-prevent-keypress (fn [e]
                                                             (if (= 13 (.-keyCode e))
                                                               (app-swap! [:input] "enter!")))})
         "Another text input"
         (d/Input [:input] "text" {:class "form-control"})
         "Textarea"
         (d/Input [:input] "textarea" {:class "form-control"})
         "Checkbox"
         (d/Input [:input] "checkbox" {:class "form-control"})
         "Select"
         (d/Input [:input] "select" {:class "form-control"
                                     :options [["one" "the first 'one'"]
                                               [{:some "value"} "keys are any value"]
                                               ["one" "more than 'one' entry with the same value"]
                                               [true "yep it's true"]
                                               [nil "this is nil"]]})))

(hb/mount App js/document.body)
