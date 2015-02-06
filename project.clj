(defproject grimoire-fxos "0.1.0-SNAPSHOT"
  :description "A Firefox Twitter client extends Grimoire"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2498"]
                 [prismatic/dommy "1.0.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [hipo "0.2.0"]]
  :source-paths ["src/clj"]
  :cljsbuild {
    :builds [{
      ;ソースコードを配置するパス
      :source-paths ["src/cljs"]
      ;nodejsか標準を選択
      ;ClojureScriptコンパイラの基本オプション
      ;(ClojureScriptのドキュメントを良く見てね)
      :compiler {
        ;標準設定: target/cljsbuild-main.js
        :optimizations :simple
        :output-to "app/js/app.js"
        :externs ["jslib/violet_externs.js"]
        :pretty-print true}}]})
