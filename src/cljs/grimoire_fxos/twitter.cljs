(ns grimoire-fxos.twitter
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [put! <! >! chan]]))

(def JSON js/JSON)

(defn add-account!
  [violet account]
  (.. violet -accounts (add (. account -accountId)
                            (. account -screenName)
                            (. account -accessToken)
                            (. account -acceTokenSecret))))

(defn consumer-key->violet
  [consumer-key]
  (js/Violet.
    (clj->js {:consumerKey (:key consumer-key)
              :consumerSecret (:secret consumer-key)})))

(defn save-accounts!
  [violet]
  (->> (.getList violet)
       (map #(.get violet %))
       JSON.stringify))

(defn load-accounts! 
  [violet]
  (some->> (.. js/localStorage (getItem "accounts"))
           JSON.parse
           (map #(add-account! violet %)) doall))

(defn fetch-auth-url [violet]
  (let [c (chan)]
    (go (.. violet -accounts (obtainAuthorizeURI #(put! c %) #(put! c %)))
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
  (.. violet -streaming (startUserStream listener #(print %))))

(defn stop
  [violet]
  (.. violet -streaming stopUserStream))

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

(defn reply
  [violet text target]
  (go (let [c (chan)
            data (clj->js {:status (str "@" (-> target :user :screen_name) " " text)
                           :in_reply_to_status_id (-> target :user :id)})]
        (.request violet "statuses/update" data #(put! c %) #(put! c %))
        (-> c <! js->clj))))

(defn fav
  [violet id]
  (go (let [c (chan)
            data (clj->js {:id id})]
        (.request violet "favorites/create" data #(put! c %) #(put! c %))
        (-> c <! js->clj))))

(defn unfav
  [violet id]
  (go (let [c (chan)
            data (clj->js {:id id})]
        (.request violet "favorites/destroy" data #(put! c %) #(put! c %))
        (-> c <! js->clj))))
