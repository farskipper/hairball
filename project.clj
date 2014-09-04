(defproject hairball "0.0.2-SNAPSHOT"
  :description "Your webapp should only have one hairball"
  :url "https://github.com/smallhelm/hairball"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2311"]]

  :source-paths ["src" "target/classes"]

  :profiles {:dev {:plugins [[quickie "0.2.5"]
                             [lein-cljsbuild "1.0.3"]
                             [com.keminglabs/cljx "0.4.0"]]}}


  :cljx {:builds [{:source-paths ["src"]
                   :output-path "target/classes"
                   :rules :clj}

                  {:source-paths ["src"]
                   :output-path "target/classes"
                   :rules :cljs}]}

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src" "target/classes"]
              :compiler {
                :output-to  "resources/public/js/main.js"
                :output-dir "resources/public/js/out"
                :optimizations :none
                :source-map true}}]})
