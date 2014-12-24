(ns grimoire-fxos.ui
  (:require [dommy.core :refer [listen! prepend! value]]
            [grimoire-fxos.twitter :refer [post post-with-media]]
            [cljs.core.async :refer [<!]]
            [hipo.interpreter])
  (:require-macros [dommy.core :refer [sel1 sel]]
                   [hipo :refer [create]]
                   [cljs.core.async.macros :refer [go]]))

(defn focus-new-tweet-section
  [e]
  (set! (.. js/document (querySelector "#newTweetSection") -className) "current skin-organic")
  (set! (.. js/document (querySelector "[data-position=\"current\"]") -className) "left skin-organic"))

(defn focus-back
  [e]
  (set! (.. js/document (querySelector "#newTweetSection") -className) "right skin-organic")
  (set! (.. js/document (querySelector "[data-position=\"current\"]") -className) "current skin-organic"))

(defn initialize-view!  [violet]
  (-> :#newPostButton
      sel1
      (listen! :click focus-new-tweet-section))
  (-> :#backButton
      sel1
      (listen! :click focus-back))
  (-> :#statusUpdateButton
      sel1
      (listen! 
        :click #(let [file (-> :#file sel1 .-files (aget 0))]
                 (go (if-not (nil? file)
                   (-> violet (post-with-media (-> :#newTweetText sel1 value) file) <! print)
                   (-> violet (post (-> :#newTweetText sel1 value)) <! print)))))))

(defn add-tweet!
  [tweet]
  (->> (create [:li
                 [:a {:href "#"}
                   [:aside.tweetIcon
                     [:img {:src (-> tweet :user :profile_image_url)}]]
                   [:p ^:text (-> tweet :user :name)
                       [:em (str " @" (-> tweet :user :screen_name))]]
                   [:p.tweetText (-> tweet :text)]]])
      (prepend! (sel1 :#tweetBox))))
