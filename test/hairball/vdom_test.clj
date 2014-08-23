(ns hairball.vdom-test
  (:require [clojure.test :refer :all]
            [hairball.vdom :as d]))

(deftest test-fix-children
  (is (= ["helloworld1"]
         (d/fix-children ["hello" nil "world" 1 []])))

  (is (= ["helloworld11.2"]
         (d/fix-children ["hello" nil "world" 1 [1.2]])))

  (is (= ["helloworld" (d/span "else")]
         (d/fix-children ["hello" nil "world" [nil [[(d/span "else")] nil]]])))

  (is (= ["helloworld" (d/span "else")]
         (d/fix-children ["hello" (d/span "else") "world"])))

  (is (= [(d/span "else")]
         (d/fix-children [nil [[(d/span "else")]]]))))


