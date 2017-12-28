(defproject ui "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [org.clojure/clojurescript "1.9.671" :scope "provided"]
                 [cljsjs/react-with-addons "15.4.2-2"]
                 [com.cemerick/url "0.1.1"]
                 [reagent "0.6.1" :exclusions [cljsjs/react]]
                 [re-frame "0.9.2" :exclusions [cljsjs/react]]
                 [reagent-utils "0.2.1" :exclusions [cljsjs/react]]
                 [re-frisk "0.5.0"]
                 [binaryage/devtools "0.9.2"]
                 [cljsjs/moment-timezone "0.5.11-1"]
                 [cljs-http "0.1.43"]
                 [clj-time "0.14.0"]
                 [com.andrewmcveigh/cljs-time "0.5.0"]
                 [hiccup "1.0.5"]
                 [bouncer "1.0.1"]
                 [garden "1.3.2"]
                 [cljsjs/moment "2.17.1-1"]
                 [cljsjs/medium-editor "5.23.2-0"]
                 [route-map "0.0.4"]]

  :plugins [[lein-environ "1.1.0"]
            [lein-cljsbuild "1.1.6"]
            [lein-ancient "0.6.10"]]

  :min-lein-version "2.5.0"

  :clean-targets ^{:protect false}
  [:target-path
   [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]

  :source-paths ["src/cljs" "embed/re-form/src/cljs" "embed/re-form/src/cljc"]
  :resource-paths ["resources"]

  :figwheel
  {:http-server-root "public"
   :server-port 3000
   :nrepl-port 7003
   :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"
                      "cider.nrepl/cider-middleware"]}


  :profiles {:dev {:repl-options {:init-ns ui.repl
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   :dependencies [[ring/ring-mock "0.3.0"]
                                  [ring/ring-devel "1.5.1"]
                                  [prone "1.1.4"]
                                  [re-frisk "0.5.0"]
                                  [matcho "0.1.0-RC6"]
                                  [figwheel-sidecar "0.5.10-SNAPSHOT"]
                                  [org.clojure/tools.nrepl "0.2.13"]
                                  [com.cemerick/piggieback "0.2.2-SNAPSHOT"]
                                  [cider/cider-nrepl "0.15.1"]
                                  [pjstadig/humane-test-output "0.8.1"]]

                   :source-paths ["env/dev/cljs" "env/dev/clj" "src/cljs" "embed/re-form/src/cljs" "embed/re-form/src/cljc"]
                   :plugins [[lein-figwheel "0.5.10-SNAPSHOT"]]

                   :cljsbuild
                   {:builds
                    {:ui {:source-paths ["env/dev/cljs" "env/dev/clj" "src/cljs" "embed/re-form/src/cljs" "embed/re-form/src/cljc"]
                          :compiler
                          {:main "ui.dev"
                           :preloads [re-frisk.preload]
                           :asset-path "/js/out"
                           :output-to "resources/public/js/ui.js"
                           :output-dir "resources/public/js/out"
                           :source-map true
                           :optimizations :none
                           :pretty-print  true
                           }}}}}

             :staging {:dependencies [[ring/ring-mock "0.3.0"]
                                      [ring/ring-devel "1.5.1"]
                                      [prone "1.1.4"]
                                      [re-frisk "0.5.0"]
                                      [figwheel-sidecar "0.5.10-SNAPSHOT"]
                                      [org.clojure/tools.nrepl "0.2.13"]
                                      [com.cemerick/piggieback "0.2.2-SNAPSHOT"]
                                      [pjstadig/humane-test-output "0.8.1"]]
                       :source-paths ["env/staging/cljs"]
                       :cljsbuild
                       {:builds
                        {:ui {:source-paths ["env/staging/cljs"]
                              :verbose true
                              :compiler
                              {:main "ui.staging"
                               :verbose true
                               :output-to "build/js/ui.js"
                               :optimizations :advanced
                               :pretty-print  false}}}}}

             :prod {:cljsbuild
                    {:builds
                     {:ui {:source-paths ["env/prod/cljs"]
                           :verbose true
                           :compiler
                           {:main "ui.prod"
                            :verbose true
                            :output-to "build/js/ui.js"
                            :optimizations :advanced
                            :pretty-print  false}}}}}})
