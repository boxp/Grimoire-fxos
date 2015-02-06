(ns grimoire-fxos.twitter
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [put! <! >! chan]]))

(def JSON js/JSON)

(defn add-account!
  [violet account]
  (.. violet -accounts (add (get account "accountId")
                            (get account "screenName")
                            (get account "accessToken")
                            (get account "accessTokenSecret"))))

(defn consumer-key->violet
  [consumer-key]
  (js/Violet.
    (clj->js {:consumerKey (:key consumer-key)
              :consumerSecret (:secret consumer-key)})))

(defn save-accounts!
  [violet]
  (as-> (.. violet -accounts getList) $
        (map #(.. violet -accounts (get %)) $)
        (into-array $)
        (JSON.stringify $)
        (.. js/localStorage (setItem "accounts" $))))

(defn load-accounts! 
  [violet]
  (let [accounts (.. js/localStorage (getItem "accounts"))]
    (when accounts
      (some->> accounts
               JSON.parse
               js->clj
               (map #(add-account! violet %)) doall)
      violet)))

(defn- fetch-auth-url [violet]
  (let [c (chan)]
    (go (.. violet -accounts requestAuthorizeURI (then #(put! c %) #(put! c %)))
        (<! c))))

(defn verify-account!
  [violet]
  (go (let [c (chan)
            url (<! (fetch-auth-url violet))
            activity (js/MozActivity. 
                       (clj->js {:name "view"
                                 :data {:type "url"
                                        :url url}}))
            pin (js/prompt "Please enter your PIN: " "")]
        (.. violet -accounts (addWithPIN pin) (then #(put! c :succeed) #(put! c :failed)))
        (println "Verify pin code: " (<! c))
        (save-accounts! violet)
        violet)))

(defn start!
  "Recognize listeners and start UserStream
   Example: (start! violet 39393939 {:tweet println})"
  [violet account-id listeners]
  (doall (map 
           #(.. violet -streaming (on (-> % key name)
                                      (val %)))
           listeners))
  (.. violet -streaming (startUserStream account-id)))

(defn stop
  [violet]
  (.. violet -streaming stopUserStream))

(defn post
  [violet text]
  (go (let [c (chan)
            data (clj->js {:status text})]
        (.. violet -rest -statuses -update 
                   (on "success" #(put! c %)))
        (.. violet -rest -statuses (update data))
        (-> c <! js->clj))))

(defn post-with-media
  [violet text media]
  (go (let [c (chan)
            data (clj->js {"status" text
                           "media[]" media})]
        (.. violet -rest -media -upload 
                   (on "success" #(put! c %)))
        (.. violet -rest -media (upload data))
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
