(ns app.model
  (:require [app.event-source :as evs]
            [org.httpkit.server :as server]
            [cheshire.core :as json]
            [app.pg :as pg]))

(defn migrate [db]
  (pg/exec db "CREATE SEQUENCE if not exists tx")
  (pg/exec db "create table if not exists rooms    (id serial primary key, tx bigint, resource jsonb)")
  (pg/exec db "create table if not exists messages (id serial primary key, tx bigint, room_id bigint, resource jsonb)"))

(defonce room-subscription (atom []))
(defonce messages (atom {}))
(defonce users (atom {}))

(defn $clear-connection [ch]
  (println "TODO remove related from users and related room-subscription"))

(defn rooms-sub [{row :row tbl :table :as event}]
  (println "ROOM SUB: " row)
  (when (= :messages tbl)
    (doseq [[current-user-id current-room request] @room-subscription]
      (when (= current-room (str (get-in event [:row :room_id])))
        (let [message (merge (:resource row) (dissoc row :resource))
              {channel :channel} (get @users current-user-id)
              request {:success (keyword (:success request))}
              resp {:body [message] :request request :status 200}]
          (server/send! channel (json/generate-string resp)))))))

(when-not (evs/has-sub? :rooms)
  (evs/add-sub :rooms #'rooms-sub)

  )

(defn next-tx [db]
  (-> (pg/q db "SELECT nextval('tx');")
      first
      :nextval))

(defn create-room [db room]
  (pg/jsonb-insert db :rooms {:resource room}))

(defn get-rooms [db]
  (pg/jsonb-query db {:select [:*] :from [:rooms]}))

(defn $get-rooms [{db :db :as req}]
  {:body (get-rooms db)})

(defn $get-room [req]
  {:body {:memebers [1 2 3]}})

(defn $subscribe-messages [{{user-id :user-id} :body {room :id} :route-params :as request}]
  (when-not (some #(= % [user-id room]) @room-subscription)
    (swap! room-subscription conj [user-id room request]))
  {:status 200 :body []})


(defn $register [{channel :channel {user-id :user-id name :name :as body} :body :as params}]
  (swap! users assoc user-id (assoc body :channel channel))
  {:body []})

(defn create-message [db message]
  (pg/jsonb-insert db :messages {:room_id (:room_id message)
                                 :resource message}))

(defn $add-message [{db :db {user-id :user-id text :text} :body {room :id} :route-params :as data}]
  (let [{name :name} (get @users user-id)
        message {:author name :message text :room_id room}]
    {:status 200
     :body (create-message db message)}))

(defn $get-messages [{db :db {room :id} :route-params}]
  {:body (pg/jsonb-query db {:select [:*] :from [:messages] :where [:= :room_id room]})})


(comment
  (pg/exec pg/db "create table if not exists rooms    (id serial primary key, tx bigint, resource jsonb)")

  (pg/exec pg/db "delete from rooms")

  (get-rooms pg/db)

  (next-tx pg/db)

  (evs/add-sub :rooms
               (fn [x]
                 (when (= :rooms (:table x))
                   (println "ON CHANGE" x))))

  (evs/rm-sub :rooms)

  (create-room pg/db {:title "Another room 2"})
  (create-message pg/db {:room_id 10
                         :message "Hello all"})

  (migrate pg/db)


  (do
    (reset! users {})
    (reset! room-subscription [])
    (reset! messages {}))

  @room-subscription





  )

