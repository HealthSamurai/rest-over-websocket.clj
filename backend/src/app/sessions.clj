(ns app.sessions
  (:require [org.httpkit.server :as server]
            [cheshire.core :as json]
            [clj-yaml.core :as yaml]
            [route-map.core :as routing]
            [app.pg :as pg]
            [app.event-source :as evs]
            [org.httpkit.server :as server]))

(defonce state (atom {:channels {}
                      :sessions {}
                      :subs {}}))

(comment
  (:subs @state)
  (:sessions @state)
  )

(defn add-session [ch info]
  (let [sess-id (str (gensym))]
    (swap! state (fn [st]
                   (-> st
                       (assoc-in [:channels ch] sess-id)
                       (assoc-in [:sessions sess-id] {:channel ch :info info}))))
    sess-id))


(defn rm-session [ch]
  (swap! state (fn [st]
                 (let [sess-id (get-in st [:channels ch])]
                   (-> st
                       (update :channels dissoc ch)
                       (update :sessions dissoc sess-id)
                       (update :subs
                               (fn [subs]
                                 (reduce (fn [subs [pth sids]]
                                           (assoc subs pth (reduce (fn [sids [sid v]]
                                                                     (if (= sid sess-id)
                                                                       sids
                                                                       (assoc sids sid v))) {} sids))
                                           ) {} subs))))))))

(defn add-subs [sub-id sess-id info]
  (swap! state assoc-in
         [:subs sub-id sess-id]
         info))

(defn rm-subs [sub-id sess-id]
  (swap! state update-in
         [:subs sub-id]
         dissoc
         sess-id))

(defn clear-subs [sub-id]
  (defn rm-subs [sub-id sess-id]
    (swap! state update
           :subs
           dissoc
           sub-id)))

(defn notify [sub-id msg]
  (println "notify:" sub-id msg)
  (doseq [[sess-id req] (get-in @state [:subs sub-id])]
    (if-let [{ch :channel} (get-in @state [:sessions sess-id])]
      (let [resp (assoc msg :request req)]
        (println "send to" resp ch)
        (server/send! ch (json/generate-string resp)))
      (println "WARN no channel for " sess-id))))

(defn send-to [sess-id msg])

(comment
  (get @state [:sessions])
  )
