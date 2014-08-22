(defproject hairball "0.0.1-SNAPSHOT"
  :description "Keep your webapp from turning into a giant hairball"
  :url "https://github.com/smallhelm/hairball"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2311"]]
  :source-paths ["src"]

  :profiles {:dev {:plugins [[quickie "0.2.5"]
                             [lein-cljsbuild "1.0.3"]]}}

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :compiler {
                :output-to  "resources/public/js/main.js"
                :output-dir "resources/public/js/out"
                :optimizations :none
                :source-map true}}]})
