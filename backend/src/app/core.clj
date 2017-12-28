(ns app.core
  (:require [org.httpkit.server :as server]
            [cheshire.core :as json]
            [clj-yaml.core :as yaml]
            [route-map.core :as routing]
            [app.pg :as pg]
            [app.event-source :as evs]
            [app.model :as model]))

(defn dispatch [req]
  {:status 200
   :body "hello"})


(def routes
  {:GET          {:action :index}
   "$conn"       {:GET {:action :connection}}
   "register"    {:POST {:action :register}}
   "rooms"       {:GET  {:action :rooms}
                  [:id] {:GET {:action :room}
                         "messages" {:GET {:action :messages}
                                     :SUB {:action :subscribe-messages}}
                         :POST {:action :add-message}}}})

(defn index [_] {:body routes})

(defonce connections (atom #{}))

(declare *dispatch)

(defn dispatch-socket-request [req]
  (println "dispatch" req)
  (*dispatch req))

(defn connection-handler [ctx]
  (server/with-channel ctx ch
    (swap! connections conj ch)
    (server/on-close
     ch (fn [status]
          (swap! connections disj ch)
          (println "channel closed: " status)))
    (server/on-receive
     ch (fn [data]
          (let [req  (json/parse-string data keyword)
                reqs (if (map? req) [req] req)]
            (doseq [r reqs]
              (let [resp (dispatch-socket-request (merge ctx (assoc r :channel ch)))]
                (server/send! ch (json/generate-string (assoc resp "request" r))))))))))


(def fns-map {:index index
              :register #'model/$register
              :rooms #'model/$get-rooms
              :room #'model/$get-room
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
  (let [db (:db cfg)
        ev-conn (evs/connection (:ev cfg))
        ctx {:db db :ev ev-conn}
        stack (-> *dispatch format-mw)
        web-h (fn [req] (stack (merge ctx req)))
        web (server/run-server web-h {:port 8080})]
    (assoc ctx :web web)))

(defn stop [{web :web ev :ev}]
  (web)
  (evs/close-connection ev))

(comment
  (def srv (start {:db {:dbtype "postgresql"
                        :connection-uri "jdbc:postgresql://localhost:5444/postgres?stringtype=unspecified&user=postgres&password=secret"}
                   :ev {:uri "jdbc:postgresql://localhost:5444/postgres"
                        :user "postgres"
                        :password "secret"
                        :slot "test_slot"
                        :decoder "wal2json"}}))

  srv

  ;; stop it
  (stop srv)

  (server/send!
   (first @connections)
   "hello world !!!")

  (do
    (reset! users {})
    (reset! room-subscription [])
    (reset! messages {}))

  @room-subscription



  )
