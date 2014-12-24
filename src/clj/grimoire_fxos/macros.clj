(ns grimoire-fxos.macros
  (:require [clojure.java.io :refer [resource]]))

(defmacro defkey
  [symbol-name file-name]
  (let [content (-> file-name resource .getPath load-file)]
    `(def ~symbol-name
      ~content)))
