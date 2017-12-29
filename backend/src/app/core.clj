(ns app.core
  (:require [org.httpkit.server :as server]
            [cheshire.core :as json]
            [clj-yaml.core :as yaml]
            [route-map.core :as routing]
            [app.pg :as pg]
            [app.event-source :as evs]
            [app.sessions :as sessions]
            [app.model :as model])
  (:gen-class))

(defn dispatch [req]
  {:status 200
   :body "hello"})


(def routes
  {:GET          {:action :index}
   "$conn"       {:GET {:action :connection}}
   "register"    {:POST {:action :register}}
   "rooms"       {:GET  {:action :rooms}
                  :POST {:action :create-room}
                  :SUB  {:action :sub-room}
                  [:id] {:GET {:action :room}
                         "messages" {:GET {:action :messages}
                                     :SUB {:action :subscribe-messages}}
                         :POST {:action :add-message}}}})

(defn index [_] {:body routes})

(defonce connections (atom #{}))

(declare *dispatch)

(defn dispatch-socket-request [req]
  (*dispatch req))

(defn connection-handler [ctx]
  (server/with-channel ctx ch
    (let [sess-id (sessions/add-session ch {})]
      (server/on-close
       ch (fn [status]
            (swap! connections disj ch)
            (sessions/rm-session ch)
            (println "channel closed: " status)))
      (server/on-receive
       ch (fn [data]
            (let [req  (json/parse-string data keyword)
                  reqs (if (map? req) [req] req)]
              (doseq [r reqs]
                (let [resp (dispatch-socket-request (merge ctx (assoc r :channel ch :session-id sess-id)))]
                  (server/send! ch (json/generate-string (assoc resp "request" r)))))))))))


(def fns-map {:index index
              :register #'model/$register
              :rooms #'model/$get-rooms
              :room #'model/$get-room
              :create-room #'model/$create-room
              :sub-room #'model/$sub-room
              :messages #'model/$get-messages
              :add-message #'model/$add-message
              :subscribe-messages #'model/$subscribe-messages
              :connection #'connection-handler})

(defn format-mw [f]
  (fn [req]
    (let [resp (f req)]
      (if (and (:body resp) (coll? (:body resp)))
        (update resp :body json/generate-string)
        ;;(update resp :body yaml/generate-string)
        resp))))

(defn *dispatch [{uri :uri meth :request-method :as req}]
  (if-let [m (routing/match [meth uri] #'routes)]
    (let [req (assoc req :current-route (:match m) :route-params (:params m))]
      (if-let [h (get fns-map (:action (:match m)))]
        (h req)
        {:status 404 :body {:message (str "No implementation for " (:match m))}}))
    {:status 404 :body {:message (str uri " is not found")}}))

(defn start [cfg]
  (println "Migrate")

  (model/migrate (:db cfg))
  (let [db (:db cfg)
        ev-conn (evs/connection (:ev cfg))
        ctx {:db db :ev ev-conn}
        stack (-> *dispatch format-mw)
        web-h (fn [req] (stack (merge ctx req)))
        web (server/run-server web-h {:port (or (:port cfg) 8080)})]
    (println "Web started on " (or (:port cfg) 8080))
    (println "test connection "
             (pg/q (:db cfg) "select 1"))
    (assoc ctx :web web)))

(defn stop [{web :web ev :ev}]
  (web)
  (evs/close-connection ev))

(defn connection-string []
  (str "jdbc:postgresql://"
       (System/getenv "PGHOST")
       ":"
       (System/getenv "PGPORT")
       "/"
       (System/getenv "PGDATABASE")))

(defn connection-string-with-params []
  (str (connection-string)
       "?stringtype=unspecified&user="
       (System/getenv "PGUSER")
       "&password="
       (System/getenv "PGPASSWORD")))

(defn env-config []
  {:port (System/getenv "PORT")
   :db {:dbtype "postgresql"
        :connection-uri (connection-string-with-params)}
   :ev {:uri (connection-string)
        :user (System/getenv "PGUSER")
        :password (System/getenv "PGPASSWORD")
        :slot "test_slot"
        :decoder "wal2json"}})

(defn -main [& args]
  (start (env-config)))

(connection-string)
(connection-string-with-params)

(comment
  (def srv (start (env-config)))

  srv

  ;; stop it
  (stop srv)


  )
