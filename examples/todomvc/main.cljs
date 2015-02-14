(ns hairball.example.main
  (:require [hairball.core :as hb]
            [hairball.vdom :as d :include-macros true]
            [hairball.app :refer [app-get app-swap!]]))

(app-swap! [:items] [{:text "one"} {:done true  :text "two"}])
(app-swap! [:selected-state] "All")

(defn get-n-left []
  (count (remove :done (app-get [:items]))))

(defn get-n-completed []
  (count (filter :done (app-get [:items]))))

(defn drop-nth [n coll]
  (let [v (if (vector? coll)
            coll
            (vec coll))]
    (vec (concat (subvec v 0 n) (subvec v (inc n) (count v))))))

(defn clear-completed []
  (app-swap! [:items] (into [] (remove :done (app-get [:items])))))

(defn onclick-rm-item [i]
  (fn []
    (app-swap! [:items] (drop-nth i (app-get [:items])))))

(defn add-new-item []
  (do
    (app-swap! [:items] (conj (app-get [:items]) {:text (app-get [:new-item])}))
    (app-swap! [:new-item] "")))

(defn Item [i _]
  (d/li {:class (if (app-get [:items i :done]) "completed")}
        (d/div {:class "view"}
               (d/Input [:items i :done] "checkbox" {:class "toggle"})
               (d/label (app-get [:items i :text]))
               (d/button {:class "destroy"
                          :on-click (onclick-rm-item i)}))))


(defn App []
  (d/div {}
         (d/section {:class "cssid-todoapp"}
                    (d/header {:class "cssid-header"}
                              (d/h1 "todos")
                              (d/Input [:new-item] "text" {:class "cssid-new-todo"
                                                           :placeholder "What needs to be done?"
                                                           :autofocus true
                                                           :on-no-prevent-keypress (fn [e]
                                                                                     (if (= 13 (.-keyCode e))
                                                                                       (add-new-item)))}))
                    (if (not-empty (app-get [:items]))
                      [(d/section {:class "cssid-main"}
                                  (d/Input [:toggle-all] "checkbox" {:class "cssid-toggle-all"})
                                  (d/label {:for "toggle-all"} "Mark all as complete")
                                  (d/ul {:class "cssid-todo-list"}
                                        (map-indexed Item
                                                     (filterv (fn [item]
                                                                (case (app-get [:selected-state])
                                                                  "Active"    (not (:done item))
                                                                  "Completed" (:done item)
                                                                  true))
                                                              (app-get [:items])))))
                       (d/footer {:class "cssid-footer"}
                                 (d/span {:class "cssid-todo-count"}
                                         (d/strong (get-n-left)) " items left")
                                 (d/ul {:class "cssid-filters"}
                                       (map (fn [state]
                                              (d/li (d/a {:href "#"
                                                          :on-click (fn []
                                                                      (app-swap! [:selected-state] state))
                                                          :class (if (= state (app-get [:selected-state]))
                                                                   "selected")}
                                                         state))) ["All" "Active" "Completed"]))
                                 (if (< 0 (get-n-completed))
                                   (d/button {:class "cssid-clear-completed"
                                              :on-click clear-completed}
                                             "Clear Completed")))]))
         (d/pre (pr-str (app-get [])))
         (d/footer {:class "cssid-info"}
                   (d/p "Double-click to edit a todo")
                   (d/p "Created by " (d/a {:href "https://github.com/smallhelm"} "Small Helm LLC"))
                   (d/p "Part of " (d/a {:href "http://todomvc.com"} "TodoMVC")))))

(hb/mount App js/document.body)
