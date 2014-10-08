(ns hairball.core-test
  (:require [clojure.test :refer :all]
            [hairball.core :refer :all]
            [hairball.vdom :as d])
  (:import [hairball.core JSop]))

(deftest test-escape-html
  (is (= "&lt;br&gt;" (escape-html "<br>")))
  (is (= "bob says &quot;hi&quot;." (escape-html "bob says \"hi\".")))
  (is (= "&lt;script&gt;alert(&apos;XSS!&apos;);&lt;/script&gt;"
         (escape-html "<script>alert('XSS!');</script>"))))

(deftest test-cleanup-attrs
  (is (= {:id "0.1"} (cleanup-attrs [0 1] (d/a {:id "css-ids-are-evil"}))))
  (is (= {} (cleanup-attrs [0 1] (d/a {:id "css-ids-are-evil"}) false)))
  (is (= {:id "0.1"} (cleanup-attrs [0 1] (d/a {:on-click (fn [] 1)})))))


(deftest test-vdom->string

  ;testing arguments to the function
  (is (= "<br id=\"root\"/>" (vdom->string (d/br))))
  (is (= "<br id=\"asdf.1\"/>" (vdom->string (d/br) ["asdf" 1])))
  (is (= "<br/>" (vdom->string (d/br) ["asdf" 1] false)))
  (is (= (str "<br data-hairball-hash=\"" (hash (d/br)) "\"/>") (vdom->string (d/br) ["asdf" 1] false true)))

  (is (= "<div></div>" (vdom->string (d/div {:class nil}) [0] false)))

  (is (= "<div class=\"something\">elseok</div>" (vdom->string (d/div {:class "something"} "else" "ok") [0] false)))
  (is (= "<ul><li>one</li><li>two</li></ul>" (vdom->string (d/ul (d/li "one")
                                                                 (d/li "two")) [0] false)))

  (testing "list of vdoms (children)"
    (is (= "" (vdom->string [] [0] false)))
    (is (= "<b>one</b>" (vdom->string [(d/b "one")] [0] false)))
    (is (= "<b>one</b><b>two</b>" (vdom->string [(d/b "one") (d/b "two")] [0] false))))

  (testing "non vdom input"
    (is (= "some string" (vdom->string "some string")))

    (is (= "&lt;script&gt;alert(&apos;XSS!&apos;);&lt;/script&gt;"
           (vdom->string "<script>alert('XSS!');</script>"))))

  (testing "rubish"
    (is (= "" (vdom->string nil)))
    (is (= "" (vdom->string {})))
    (is (= ":somedata" (vdom->string {:some "data"})))
    (is (= "" (vdom->string (fn [x] (+ x 1)))))))


(deftest test-vdoms->JSops
  (is (= [(JSop. :replace-node ["root"] [(d/span)])]
         (vdoms->JSops (d/div)
                       (d/span))))

  (is (= [] (vdoms->JSops (d/div) (d/div))));no change => no ops

  (is (= [(JSop. :set-attribute ["root"] [:class "after"])]
         (vdoms->JSops (d/div {:class "before"})
                       (d/div {:class "after"}))))

  (is (= [(JSop. :set-content ["root"] ["after"])]
         (vdoms->JSops (d/div "before")
                       (d/div "after"))))

  (is (= [(JSop. :set-content ["root"] [""])
          (JSop. :insert-child  ["root"] [(d/b "after") 0])]
         (vdoms->JSops (d/div "before")
                       (d/div (d/b "after")))))


  (is (= [(JSop. :insert-child ["root"] [(d/li "3") 2])]
         (vdoms->JSops (d/ul
                        (d/li "1")
                        (d/li "2"))
                       (d/ul
                        (d/li "1")
                        (d/li "2")
                        (d/li "3")))))


    (is (= [(JSop. :remove-node ["root" 2] [])]
         (vdoms->JSops (d/ul
                        (d/li "1")
                        (d/li "2")
                        (d/li "3"))
                       (d/ul
                        (d/li "1")
                        (d/li "2")))))


  (is (= [(JSop. :set-content ["root" 1] ["something long"])]
         (vdoms->JSops (d/ul
                        (d/li "1")
                        (d/li "2"))
                       (d/ul
                        (d/li "1")
                        (d/li "something long")))))


  (is (= [(JSop. :set-attribute ["root"] [:class "party"])
          (JSop. :set-content ["root"] ["on"])]
         (vdoms->JSops (d/div)
                       (d/div {:class "party"} "on"))))

    (is (= [(JSop. :remove-attribute ["root"] [:class])]
         (vdoms->JSops (d/div {:class "before"})
                       (d/div))))

  (is (= [(JSop. :remove-attribute ["root" 0] [:colspan])
          (JSop. :set-content ["root" 0] ["data 1"])
          (JSop. :insert-child ["root"] [(d/td "data 2") 1])
          (JSop. :insert-child ["root"] [(d/td "data 3") 2])]
         (vdoms->JSops (d/tr
                        (d/td {:colspan 3}
                              "- no data- "))
                       (d/tr
                        (d/td "data 1")
                        (d/td "data 2")
                        (d/td "data 3")))))


  ;;
  ;;the following test is correct but can be optimized by greatest common sequence algorithm or something
  ;;
  (is (= [(JSop. :set-content ["root" 1] ["2"])
          (JSop. :insert-child ["root"] [(d/li "3") 2])]
         (vdoms->JSops (d/ul
                        (d/li "1")
                        (d/li "3"))
                       (d/ul
                        (d/li "1")
                        (d/li "2")
                        (d/li "3")))))

  (is (= [(JSop. :set-attribute ["root" 1] [:class "alert"])
          (JSop. :remove-node ["root" 1 0] [])
          (JSop. :set-content ["root" 1] ["error!"])
          (JSop. :insert-child ["root"] [(d/div
                                          (d/a {:href "google.com"} "google")) 2])]
         (vdoms->JSops (d/div
                        (d/b "hello")
                        (d/div
                         (d/a {:href "google.com"} "google")))
                       (d/div
                        (d/b "hello")
                        (d/div {:class "alert"} "error!")
                        (d/div
                         (d/a {:href "google.com"} "google")))))))
