(ns grimoire-fxos.twitter
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [put! <! >! chan]]))

(defn consumer-key->violet
  [consumer-key]
  (.init js/Violet
    (clj->js {:consumer_key (:key consumer-key)
              :consumer_secret (:secret consumer-key)})))

(defn access-token->violet
  [access-token consumer-key]
  (.init js/Violet
    (clj->js {:consumer_key (:key consumer-key)
              :consumer_secret (:secret consumer-key)
              :access_token (:key access-token)
              :access_token_secret (:secret access-token)})))

(defn save-access-token
  [access-token-key access-token-secret]
  (.. js/localStorage (setItem "accessTokenKey" access-token-key))
  (.. js/localStorage (setItem "accessTokenSecret" access-token-secret)))

(defn load-access-token []
  (let [access-token-key (.. js/localStorage (getItem "accessTokenKey"))
        access-token-secret (.. js/localStorage (getItem "accessTokenSecret"))]
    (if access-token-key
      {:key access-token-key
       :secret access-token-secret})))

(defn fetch-auth-url [violet]
  (let [c (chan)]
    (go (.. violet -oauth (obtainAuthorizeURI #(put! c %) #(put! c %)))
        (<! c))))

(defn fetch-access-token!
  [violet]
  (go (let [c (chan)
            url (<! (fetch-auth-url violet))
            activity (js/MozActivity. 
                       (clj->js {:name "view"
                                 :data {:type "url"
                                        :url url}}))
            pin (js/prompt "Please enter your PIN: " "")]
        (.. violet -oauth (obtainAccessToken pin #(go 
                                                   (set! (.-access_token violet) (. % -oauth_token))
                                                   (set! (.-access_tokensecret violet) (. % -oauth_token_secret))
                                                   (save-access-token (. violet -access_token)
                                                                      (. violet -access_tokensecret))
                                                   (put! c violet))
                                                #(put! c violet)))
        (<! c))))

(defn start
  [violet listener]
  (.. violet -streaming (start listener #(print %))))

(defn stop
  [violet]
  (.. violet -streaming stop))

(defn post
  [violet text]
  (go (let [c (chan)
            data (clj->js {:status text})]
        (.request violet "statuses/update" data #(put! c %) #(put! c %))
        (-> c <! js->clj))))

(defn post-with-media
  [violet text media]
  (go (let [c (chan)
            data (clj->js {"status" text
                           "media[]" media})]
        (print data)
        (.request violet "statuses/update_with_media" data #(put! c %) #(put! c %))
        (-> c <! js->clj))))
