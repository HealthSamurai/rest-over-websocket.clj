(ns app.core
  (:require [org.httpkit.server :as server]
            [cheshire.core :as json]
            [clj-yaml.core :as yaml]
            [route-map.core :as routing]))

(defn dispatch [req]
  {:status 200
   :body "hello"})

(defn get-rooms [req]
  {:body [{:id "clojure" :title "Clojure"}
          {:id "haskel" :title "Haskel"}]})

(defn get-room [req]
  {:body {:memebers [1 2 3]}})

(defonce messages (atom {}))

(defn get-messages [{{room :id} :route-params}]
  {:body (get @messages room [])})

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

(defn connection-handler [req]
  (server/with-channel req ch
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
              (let [resp (dispatch-socket-request (assoc r :channel ch))]
                (server/send! ch (json/generate-string (assoc resp "request" r))))))))))

(defonce room-subscription (atom []))

(defn subscribe-messages [{{user-id :user-id} :body {room :id} :route-params}]
  (when-not (some #(= % [user-id room]) @room-subscription)
    (swap! room-subscription conj [user-id room]))
  {:status 200 :body []})

(defonce users (atom {}))

(defn register [{channel :channel {user-id :user-id name :name :as body} :body :as params}]
  (swap! users assoc user-id (assoc body :channel channel))
  {:body []})

(defn add-message [{{user-id :user-id text :text} :body {room :id} :route-params :as data}]
  (let [{name :name} (get @users user-id)
        message {:author name :message text}]
    (swap! messages update room #(conj % message))
    (doseq [[current-user-id current-room] @room-subscription]
      (when (= current-room room)
        (let [{channel :channel} (get @users current-user-id)
              resp {:body [message] :request {:success :room-messages-change} :status 200}]
          (server/send! channel (json/generate-string resp))))))
  {:status 200})

(def fns-map {:index index
              :register #'register
              :rooms #'get-rooms
              :room #'get-room
              :messages #'get-messages
              :add-message #'add-message
              :subscribe-messages #'subscribe-messages
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

(def dispatch (-> *dispatch format-mw))
;; format-mw

(defn start []
  (server/run-server #'dispatch {:port 8080}))

(comment
  (def srv (start))
  ;; stop it
  (srv)

  (server/send!
   (first @connections)
   "hello world !!!")

  (do
    (reset! users {})
    (reset! room-subscription [])
    (reset! messages {}))

  @room-subscription



  )
