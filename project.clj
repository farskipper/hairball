(defproject hairball "0.0.2-SNAPSHOT"
  :description "Your webapp should only have one hairball"
  :url "https://github.com/smallhelm/hairball"
  :scm {:name "git"
        :url "https://github.com/smallhelm/hairball"}
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2311"]]

  :jar-exclusions [#"\.cljx|\.svn|\.swp|\.swo|\.DS_Store"]
  :resource-paths ["target/generated/classes"]
  :source-paths   ["src"]



  :profiles {:dev {:plugins [[quickie "0.2.5"]
                             [lein-cljsbuild "1.0.3"]
                             [com.keminglabs/cljx "0.4.0"]
                             [lein-pdo "0.1.1"]]

                   :cljx {:builds [{:source-paths ["src"]
                                    :output-path "target/classes"
                                    :rules :clj}
                                   {:source-paths ["src"]
                                    :output-path "target/generated/classes"
                                    :rules :cljs}]}
                   :hooks [cljx.hooks]

                   :cljsbuild {:builds [{:id "dev"
                                         :source-paths ["src" "target/classes" "target/generated/classes"]
                                         :compiler {:output-to  "resources/public/js/main.js"
                                                    :output-dir "resources/public/js/out"
                                                    :optimizations :none
                                                    :source-map true}}]}

                   :aliases {"dev"    ["pdo" "cljx" "auto," "cljsbuild" "auto," "quickie"]
                             "deploy" ["do" "clean," "deploy" "clojars"]}}})
