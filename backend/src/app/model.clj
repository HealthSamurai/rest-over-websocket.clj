(ns app.model
  (:require [app.db :as db]
            [cheshire.core :as json]
            [app.pg :as pg]))


(defn migrate [db]
  (pg/exec db "CREATE SEQUENCE if not exists tx")
  (pg/exec db "create table if not exists rooms    (id serial primary key, tx bigint, resource jsonb)")
  (pg/exec db "create table if not exists messages (id serial primary key, tx bigint, room_id bigint, resource jsonb)"))

(defn next-tx [db]
  (-> (pg/q db "SELECT nextval('tx');")
      first
      :nextval))


(defn get-rooms [db]
  (pg/q db {:select [:*] :from [:rooms]}))

(defn create-room [db room]
  (pg/q db
        {:insert-into :rooms
         :values [{:tx (pg/raw "nextval('tx')")
                   :resource (json/generate-string room)}]
         :returning [:*]}))

(comment
  (pg/exec pg/db "create table if not exists rooms    (id serial primary key, tx bigint, resource jsonb)")

  (get-rooms pg/db)

  (next-tx pg/db)

  (create-room pg/db {:name "My room"})

  (migrate pg/db)





  )

