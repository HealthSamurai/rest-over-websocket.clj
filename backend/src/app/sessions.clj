(ns app.sessions
  (:require [org.httpkit.server :as server]
            [cheshire.core :as json]
            [clj-yaml.core :as yaml]
            [route-map.core :as routing]
            [app.pg :as pg]
            [app.event-source :as evs]
            [app.model :as model]))

(defonce connections (atom {}))

(defn add-connection [ch info]
  (swap! connections assoc ch info))

(defn update-connection-info [ch info]
  (swap! connections :update ch merge info))

(defn rm-connection [ch]
  (swap! connections dissoc ch))

(defn connection-by-id [id]
  (->> @connections
       (filterv (fn [[_ {*id :id}]]
                  (= id *id)))
       ffirst))

(defn send [id message]
  (when-let [ch (connection-by-id id)]
    (server/send! ch (json/generate-string message))))
