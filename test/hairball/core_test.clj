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


(deftest test-vdom->string

  (is (= "<br/>" (vdom->string (d/br) true)))
  (is (= "<div class=\"something\">elseok</div>" (vdom->string (d/div {:class "something"} "else" "ok") true)))
  (is (= "<ul><li>one</li><li>two</li></ul>" (vdom->string (d/ul (d/li "one")
                                                                 (d/li "two")) true)))

  (testing "non vdom input"
    (is (= "some string" (vdom->string "some string")))

    (is (= "&lt;script&gt;alert(&apos;XSS!&apos;);&lt;/script&gt;"
           (vdom->string "<script>alert('XSS!');</script>")))

    (testing "rubish"
      (is (= "" (vdom->string nil)))
      (is (= "{}" (vdom->string {})))
      (is (= "{:some &quot;data&quot;}" (vdom->string {:some "data"}))))))


(deftest test-vdoms->JSops
  (is (= [(JSop. :replace-node ["root"] [(d/span)])]
         (vdoms->JSops (d/div)
                       (d/span))))

  (is (= [] (vdoms->JSops (d/div) (d/div))));no change => no ops

  (is (= [(JSop. :set-properties ["root"] [{:class "after"}])]
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


  (is (= [(JSop. :set-properties ["root"] [{:class "party"}])
          (JSop. :set-content ["root"] ["on"])]
         (vdoms->JSops (d/div)
                       (d/div {:class "party"} "on"))))


  ;;
  ;;the following test is correct but can be optimized by common sequence algorithm or something
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

  (is (= [(JSop. :set-properties ["root" 1] [{:class "alert"}])
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
