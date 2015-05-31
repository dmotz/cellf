(defproject cellf "0.1.0-SNAPSHOT"
  :description "Find yourself"
  :url "http://oxism.com/cellf"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/mit-license.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3211"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.omcljs/om "0.8.8"]]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-figwheel "0.2.9"]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]

              :figwheel {}

              :compiler {:main cellf.core
                         :asset-path "js/compiled/out"
                         :output-to "resources/public/js/compiled/cellf.js"
                         :output-dir "resources/public/js/compiled/out"
                         :optimizations :none
                         :source-map true
                         :source-map-timestamp true
                         :cache-analysis true }}
             {:id "min"
              :source-paths ["src"]
              :compiler {:output-to "resources/public/js/cellf.min.js"
                         :main cellf.core
                         :optimizations :advanced
                         :pretty-print false
                         :externs ["resources/externs/gif.ext.js"]}}]}

  :figwheel {:css-dirs ["resources/public/css"]})
