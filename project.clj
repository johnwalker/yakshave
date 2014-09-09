(defproject io.johnwalker/yakshave "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.7.0-alpha2"]
                 [org.clojure/clojurescript "0.0-2322"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  :source-paths ["src"]

  :cljsbuild {
              :builds [{:id "io.johnwalker/yakshave"
                        :source-paths ["src"]
                        :compiler {
                                   :output-to "out/io/johnwalker/yakshave.js"
                                   :output-dir "out"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}
                       {:id "prod"
                        :source-paths ["src"]
                        :compiler {:output-to "prod/yakshave.js"
                                   :output-dir "prod"
                                   :target :nodejs
                                   :optimizations :advanced}}]})
