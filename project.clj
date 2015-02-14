(defproject hairball "0.0.3-SNAPSHOT"
  :description "Your webapp should only have one hairball"
  :url "https://github.com/smallhelm/hairball"
  :scm {:name "git"
        :url "https://github.com/smallhelm/hairball"}
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.6.0"]]

  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store"]
  :resource-paths ["target/generated/classes"]
  :source-paths   ["src"]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "0.0-2760"]]
                   :plugins [[quickie "0.2.5"]
                             [lein-cljsbuild "1.0.4"]
                             [com.keminglabs/cljx "0.5.0"]
                             [lein-pdo "0.1.1"]]

                   :cljx {:builds [{:source-paths ["src"]
                                    :output-path "target/classes"
                                    :rules :clj}
                                   {:source-paths ["src"]
                                    :output-path "target/generated/classes"
                                    :rules :cljs}]}
                   :prep-tasks [["cljx"  "once"]  "javac"  "compile"]

                   :cljsbuild {:builds [{:id "sandbox"
                                         :source-paths ["src" "target/classes" "target/generated/classes" "examples/sandbox"]
                                         :compiler {:output-to  "examples/sandbox/main.js"
                                                    :output-dir "examples/sandbox/out"
                                                    :optimizations :none
                                                    :source-map true}}
                                        {:id "forms"
                                         :source-paths ["src" "target/classes" "target/generated/classes" "examples/forms"]
                                         :compiler {:output-to  "examples/forms/main.js"
                                                    :output-dir "examples/forms/out"
                                                    :optimizations :none
                                                    :source-map true}}
                                        {:id "todomvc"
                                         :source-paths ["src" "target/classes" "target/generated/classes" "examples/todomvc"]
                                         :compiler {:output-to  "examples/todomvc/main.js"
                                                    :output-dir "examples/todomvc/out"
                                                    :optimizations :none
                                                    :source-map true}}]}

                   :aliases {"dev"    ["pdo" "cljx" "auto," "cljsbuild" "auto," "quickie"]
                             "deploy" ["do"
                                       ["clean"]
                                       ["deploy" "clojars"]]}}})
