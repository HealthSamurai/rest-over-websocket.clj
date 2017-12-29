(ns ui.chat.model
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [haslett.client :as ws]
            [ui.routes :refer [href]]))


(rf/reg-event-fx
 :rooms-load
 (fn [{db :db} _]
   {:http/xhr {:uri "/rooms"
               :request-method "get"
               :success :rooms-success}}))

(rf/reg-event-db
 :rooms-success
 (fn [db [_ data]]
   (assoc db :rooms (:body data))))

(rf/reg-sub
 :rooms
 (fn [db] (:rooms db)))


(rf/reg-event-fx
 :init-chat
 (fn [{db :db} [_ rid]]
   {:http/xhr [{:uri (str "/rooms")
                :request-method "get"
                :success :rooms-loaded}
               {:uri (str "/rooms")
                :request-method "sub"
                :success :rooms-changed}]}))

(rf/reg-event-db
 :rooms-loaded
 (fn [db [_ {data :body}]]
   (assoc db :rooms data)))

(rf/reg-event-db
 :rooms-changed
 (fn [db [_ {{ch :change ent :entity :as msg} :body}]]
   (.log js/console "change:" ch ent)
   (if ent
     (update db :rooms conj ent)
     db)))

(rf/reg-sub
 :chat
 (fn [db  _] {:rooms (:rooms db)}))

(rf/reg-event-fx
 :init-messages
 (fn [{db :db} [_ rid]]
   {:http/xhr [{:uri (str "/rooms/" rid "/messages")
                :request-method "get"
                :success :messages-loaded}
               {:uri (str "/rooms/" rid "/messages")
                :request-method "sub"
                :body {:user-id "user"}
                :success :new-message}]}))

(rf/reg-event-db
 :messages-loaded
 (fn [db [_ {data :body}]]
   (assoc db :messages data)))

(rf/reg-event-db
 :new-message
 (fn [db [_ {{ch :change ent :entity :as msg} :body}]]
   (.log js/console "change:" ch ent)
   (if ent
     (update db :messages conj ent)
     db)))

(rf/reg-sub
 :messages
 (fn [db  _] (:messages db)))

(rf/reg-event-db :message-sent (fn [db _] db))

(rf/reg-event-fx
 :send-message
 (fn [{db :db} [_ rid txt]]
   {:http/xhr {:uri (str "/rooms/" rid )
               :request-method "post"
               :body {:text txt
                      :author (dissoc (:auth db) :id_token)}
               :success :message-sent}
    :dispatch [:on-change [:chat rid :input] ""]}))

(rf/reg-event-db
 :on-change
 (fn [db [_ path v]]
   (assoc-in db path v)))


(rf/reg-sub-raw
 :value
 (fn [db [_ pth]]
   (let [cur (r/cursor db pth)]
     (reaction  @cur))))

(rf/reg-sub
 :room
 (fn [db [_ rid]] (get-in db [:room rid])))

(rf/reg-event-fx
 :new-chat
 (fn [{db :db} [_ title]]
   (.log js/console "NEW:" title)
   (if title
     {:http/xhr [{:uri "/rooms/"
                  :request-method "post"
                  :body {:title title}
                  :success :chat-created}]}
     {})))

(rf/reg-event-fx
 :chat-created
 (fn [fx [_ {data :body}]]
   {:page-redirect (str "/rooms/" (:id data))}))
