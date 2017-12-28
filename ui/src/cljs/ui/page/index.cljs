(ns ui.page.index
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as rf]
            [cljs.core.async :as a :refer [<! >!]]
            [haslett.client :as ws]
            [haslett.format :as fmt]
            [ui.routes :refer [href]]
            [frames.pages :refer [reg-page]]))


(rf/reg-event-fx
 :rooms-load
 (fn [{db :db} _]
   {:http/xhr {:uri "/rooms"
               :request-method "get"
               :success :rooms-success}}))

(rf/reg-event-db
 :rooms-success
 (fn [db [_ data]]
   (.log js/console "rooms-loaded" data)
   (assoc db :rooms (:body data))))

(rf/reg-sub
 :rooms
 (fn [db] (:rooms db)))

(defn index [params]
  (rf/dispatch [:rooms-load])
  (let [rooms (rf/subscribe [:rooms])]
    (fn []
      [:div
       [:h2 "Rooms"]
       (for [r @rooms]
         [:a.item {:key (:id r) :href (href "rooms" (:id r))}
          [:b (:title r)]])])))


(rf/reg-event-fx
 :init-room
 (fn [{db :db} [_ rid]]
   {:http/xhr [{:uri (str "/rooms/" rid)
                :request-method "get"
                :success :room-success}
               {:uri (str "/rooms/" rid "/messages")
                :params {:room_id rid}
                :request-method "get"
                :success :room-messages}
               {:uri (str "/rooms/" rid "/messages")
                :params {:room_id rid}
                :request-method "sub"
                :success :room-messages-change}]}))

(rf/reg-event-db
 :room-messages
 (fn [db [_ {msgs :body}]]
   (assoc db :messages msgs)))

(rf/reg-event-db
 :room-messages-change
 (fn [db [_ {msgs :body}]]
   (update db :messages concat msgs)))

(rf/reg-sub
 :messages
 (fn [db  _] (:messages db)))

(defn show [params]
  (println "params" params)
  (rf/dispatch [:init-room (:id params)])
  (let [messages (rf/subscribe [:messages])]
    (fn []
      [:div
       [:h2 "Show"]
       [:pre (pr-str @messages)]])))

(reg-page :index/index index)
(reg-page :rooms/show show)

(comment

  (go (>! (:sink @websocket) "Hello World"))

  )
