; Using Leiningen 1.7.0 or newer:
(defproject lein-cljsbuild-example "1.2.3"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [enfocus "0.9.1-SNAPSHOT"]]
  :plugins [[lein-cljsbuild "0.2.1"]]
  :cljsbuild
  {
   :builds
   {
    ;; This build has the lowest level of optimizations, so it is
    ;; useful when debugging the app.
    :dev {:source-path "src-cljs"
          :jar true
          :compiler {:output-to "resources/public/js/main-debug.js"
                     :optimizations :whitespace
                     :pretty-print true}}
    ;; This build has the highest level of optimizations, so it is
    ;; efficient when running the app in production.
    :prod {:source-path "src-cljs"
           :compiler {:output-to "resources/public/js/main.js"
                      :optimizations :advanced
                      :pretty-print false
                      :externs ["src-cljs/externs.js"]}}}})