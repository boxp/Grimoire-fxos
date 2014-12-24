(ns grimoire-fxos.core
  (:require-macros [grimoire-fxos.macros :refer [defkey]]
                   [cljs.core.async.macros :refer [go-loop go]])
  (:require [grimoire-fxos.twitter :refer [consumer-key->violet
                                           access-token->violet
                                           load-access-token
                                           fetch-access-token!
                                           start stop]]
            [grimoire-fxos.ui :refer [initialize-view!
                                      add-tweet!]]
            [cljs.core.async :refer [<! >! chan]]
            [dommy.core :as dommy]
            [dommy.core :refer-macros [sel sel1]]))

(defkey consumer-key "consumer-key.clj")

(def tweets (atom []))

(defn status-listener
  [status]
  (as-> status $
        (js->clj $ :keywordize-keys true)
        (swap! tweets conj $)
        (last $)
        (add-tweet! $)))

(defn main []
  (go (let [violet (or (some-> (load-access-token) (access-token->violet consumer-key))
                       (-> (consumer-key->violet consumer-key)
                           fetch-access-token! <!))]
        (initialize-view! violet)
        ;; start user stream
        (start violet status-listener))))

(set! (. js/window -onload) main)
(set! *print-fn* #(.log js/console %))
