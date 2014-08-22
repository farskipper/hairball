(ns hairball.dom2-test
  (:require [clojure.test :refer :all]
            [hairball.dom2 :refer :all])
  (:import [hairball.dom2 JSop]))

(deftest test-escape-html
  (is (= "&lt;br&gt;" (escape-html "<br>")))
  (is (= "bob says &quot;hi&quot;." (escape-html "bob says \"hi\".")))
  (is (= "&lt;script&gt;alert(&apos;XSS!&apos;);&lt;/script&gt;"
         (escape-html "<script>alert('XSS!');</script>"))))


(deftest test-vdom->string

  (is (= "<br/>" (vdom->string [:br] true)))
  (is (= "<div class=\"something\"></div>" (vdom->string [:div {:class "something"}] true)))
  (is (= "<ul><li>one</li><li>two</li></ul>" (vdom->string [:ul nil [:li nil "one"] [:li nil "two"]] true)))

  (testing "non vdom input"
    (is (= "some string" (vdom->string "some string")))

    (is (= "&lt;script&gt;alert(&apos;XSS!&apos;);&lt;/script&gt;"
           (vdom->string "<script>alert('XSS!');</script>")))

    (testing "rubish"
      (is (= "" (vdom->string nil)))
      (is (= "{}" (vdom->string {})))
      (is (= "{:some &quot;data&quot;}" (vdom->string {:some "data"}))))))


(deftest test-vdoms->JSops
  (is (= [(JSop. :replace-node ["root"] [[:span]])]
         (vdoms->JSops [:div]
                       [:span])))

  (is (= [(JSop. :set-properties ["root"] [{:class "after"}])]
         (vdoms->JSops [:div {:class "before"}]
                       [:div {:class "after"}])))

  (is (= [(JSop. :set-content ["root"] ["after"])]
         (vdoms->JSops [:div {} "before"]
                       [:div {} "after"])))

  (is (= [(JSop. :set-content ["root"] [""])
          (JSop. :insert-child  ["root"] [[:b {} "after"] 0])]
         (vdoms->JSops [:div {} "before"]
                        [:div {} [:b {} "after"]])))


  (is (= [(JSop. :insert-child ["root"] [[:li {} "3"] 2])]
         (vdoms->JSops [:ul {}
                        [:li {} "1"]
                        [:li {} "2"]]
                       [:ul {}
                        [:li {} "1"]
                        [:li {} "2"]
                        [:li {} "3"]])))


    (is (= [(JSop. :remove-node ["root" 2] [])]
         (vdoms->JSops [:ul {}
                        [:li {} "1"]
                        [:li {} "2"]
                        [:li {} "3"]]
                       [:ul {}
                        [:li {} "1"]
                        [:li {} "2"]])))


  (is (= [(JSop. :set-content ["root" 1] ["something long"])]
         (vdoms->JSops [:ul {}
                        [:li {} "1"]
                        [:li {} "2"]]
                       [:ul {}
                        [:li {} "1"]
                        [:li {} "something long"]])))


  (is (= [(JSop. :set-properties ["root"] [{:class "party"}])
          (JSop. :set-content ["root"] ["on"])]
         (vdoms->JSops [:div]
                       [:div {:class "party"} "on"])))


  ;;
  ;;the following test is correct but can be optimized by common sequence algorithm or something
  ;;
  (is (= [(JSop. :set-content ["root" 1] ["2"])
          (JSop. :insert-child ["root"] [[:li {} "3"] 2])]
         (vdoms->JSops [:ul {}
                        [:li {} "1"]
                        [:li {} "3"]]
                       [:ul {}
                        [:li {} "1"]
                        [:li {} "2"]
                        [:li {} "3"]])))

  (is (= [(JSop. :set-properties ["root" 1] [{:class "alert"}])
          (JSop. :remove-node ["root" 1 0] [])
          (JSop. :set-content ["root" 1] ["error!"])
          (JSop. :insert-child ["root"] [[:div {}
                                          [:a {:href "google.com"} "google"]] 2])]
         (vdoms->JSops [:div {}
                        [:b {} "hello"]
                        [:div {}
                         [:a {:href "google.com"} "google"]]]
                       [:div {}
                        [:b {} "hello"]
                        [:div {:class "alert"} "error!"]
                        [:div {}
                         [:a {:href "google.com"} "google"]]]))))
